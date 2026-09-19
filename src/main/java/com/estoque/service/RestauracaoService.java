package com.estoque.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Restauração de banco + fotos a partir de um .zip enviado pelo admin.
 *
 * O H2 mantém o arquivo do banco aberto enquanto o app roda, então o upload só deixa o zip
 * guardado em {dados}/restore/restaurar.zip. A troca de fato acontece no PRÓXIMO início do app,
 * antes do Spring abrir o banco (ver EstoqueJavaApplication.main).
 *
 * Conteúdo aceito no zip: estoque.mv.db (obrigatório) e uploads/** (fotos). Qualquer outro caminho
 * é ignorado, e caminhos que tentam sair da pasta são recusados.
 */
public final class RestauracaoService {

    private static final Logger log = LoggerFactory.getLogger(RestauracaoService.class);

    public static final String NOME_BANCO = "estoque.mv.db";
    private static final long LIMITE_BYTES = 2L * 1024 * 1024 * 1024; // proteção contra zip bomb

    private RestauracaoService() {}

    public static Path arquivoPendente(Path dadosDir) {
        return dadosDir.resolve("restore").resolve("restaurar.zip");
    }

    /** Confere que o zip é legível e contém o banco; lança IllegalArgumentException se não. */
    public static void validar(Path zip) {
        boolean temBanco = false;
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                if (NOME_BANCO.equals(e.getName())) temBanco = true;
            }
        } catch (IOException | RuntimeException ex) {
            throw new IllegalArgumentException("Arquivo zip inválido.");
        }
        if (!temBanco)
            throw new IllegalArgumentException("O zip precisa conter o arquivo " + NOME_BANCO + " na raiz.");
    }

    /** Chamado antes de subir o Spring. Não faz nada se não há restauração pendente. */
    public static void aplicarPendente(Path dadosDir) {
        Path zip = arquivoPendente(dadosDir);
        if (!Files.isRegularFile(zip)) return;

        Path tmp = dadosDir.resolve("restore").resolve("tmp");
        try {
            deletarRecursivo(tmp);
            Files.createDirectories(tmp);
            extrair(zip, tmp);

            Path bancoNovo = tmp.resolve(NOME_BANCO);
            if (!Files.isRegularFile(bancoNovo))
                throw new IOException("zip sem " + NOME_BANCO);

            Path bancoAtual = dadosDir.resolve(NOME_BANCO);
            if (Files.exists(bancoAtual)) {
                String carimbo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                Files.move(bancoAtual, dadosDir.resolve(NOME_BANCO + ".bak-" + carimbo));
            }
            Files.move(bancoNovo, bancoAtual);

            Path fotosNovas = tmp.resolve("uploads");
            if (Files.isDirectory(fotosNovas)) copiarRecursivo(fotosNovas, dadosDir.resolve("uploads"));

            Files.delete(zip);
            log.info("Restauração aplicada: banco e fotos substituídos a partir de restaurar.zip.");
        } catch (IOException e) {
            // Mantém o zip para investigação e sobe o app com o banco que já existia.
            log.error("Falha ao aplicar restauração pendente ({}). O banco atual foi mantido.", e.getMessage());
        } finally {
            deletarRecursivo(tmp);
        }
    }

    private static void extrair(Path zip, Path destino) throws IOException {
        long total = 0;
        Path raiz = destino.toAbsolutePath().normalize();
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                String nome = e.getName().replace('\\', '/');
                boolean permitido = nome.equals(NOME_BANCO) || nome.startsWith("uploads/");
                if (!permitido) continue;

                Path alvo = raiz.resolve(nome).normalize();
                if (!alvo.startsWith(raiz)) throw new IOException("caminho inválido no zip: " + nome);

                if (e.isDirectory()) {
                    Files.createDirectories(alvo);
                    continue;
                }
                Files.createDirectories(alvo.getParent());
                total += copiarLimitado(in, alvo, LIMITE_BYTES - total);
            }
        }
    }

    private static long copiarLimitado(InputStream in, Path alvo, long restante) throws IOException {
        long escrito = 0;
        byte[] buf = new byte[64 * 1024];
        try (var out = Files.newOutputStream(alvo)) {
            int n;
            while ((n = in.read(buf)) > 0) {
                escrito += n;
                if (escrito > restante) throw new IOException("zip excede o limite de tamanho");
                out.write(buf, 0, n);
            }
        }
        return escrito;
    }

    private static void copiarRecursivo(Path origem, Path destino) throws IOException {
        try (Stream<Path> arvore = Files.walk(origem)) {
            for (Path p : (Iterable<Path>) arvore::iterator) {
                Path alvo = destino.resolve(origem.relativize(p).toString());
                if (Files.isDirectory(p)) {
                    Files.createDirectories(alvo);
                } else {
                    Files.createDirectories(alvo.getParent());
                    Files.copy(p, alvo, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deletarRecursivo(Path dir) {
        if (!Files.exists(dir)) return;
        try (Stream<Path> arvore = Files.walk(dir)) {
            arvore.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
            // limpeza best-effort
        }
    }
}
