package com.estoque.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.estoque.service.LojaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Versões menores das fotos de produto para a loja (/miniaturas/{largura}/{arquivo}).
 * As fotos originais em /uploads nunca são alteradas; as miniaturas ficam numa pasta temporária
 * e são refeitas sob demanda (por exemplo, depois de um deploy).
 * Qualquer problema — arquivo estranho, foto girada por EXIF, servidor ocupado — entrega a foto original.
 */
@RestController
public class MiniaturaController {

    private static final Logger log = LoggerFactory.getLogger(MiniaturaController.class);

    private static final Set<Integer> LARGURAS = Set.of(360, 540, 800, 1200);
    private static final Pattern NOME_SEGURO = Pattern.compile("[0-9a-fA-F-]{36}\\.(jpg|jpeg|png)");
    private static final CacheControl TRINTA_DIAS = CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic();

    /** No máximo duas fotos sendo reduzidas ao mesmo tempo (protege a memória do servidor). */
    private final Semaphore vagas = new Semaphore(2);
    private final Path originais;
    private final Path pastaMiniaturas = Path.of(System.getProperty("java.io.tmpdir"), "pafanny-miniaturas");

    private final LojaService loja;
    private final TransactionTemplate transacao;

    public MiniaturaController(@Value("${app.uploads.dir:./data/uploads}") String uploadsDir,
                               LojaService loja, PlatformTransactionManager transacoes) {
        this.originais = Path.of(uploadsDir, "produtos").toAbsolutePath().normalize();
        this.loja = loja;
        this.transacao = new TransactionTemplate(transacoes);
        this.transacao.setReadOnly(true);
    }

    /**
     * Depois que o servidor sobe, prepara aos poucos as miniaturas das fotos da vitrine (uma por vez,
     * em segundo plano), para as primeiras visitas depois de um deploy já receberem fotos leves.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void prepararMiniaturas() {
        Thread t = new Thread(() -> {
            try {
                // Fora de uma requisição web a lista de fotos precisa ser lida dentro de uma transação.
                List<String> fotos = transacao.execute(st -> loja.listarVisiveis().stream()
                    .flatMap(p -> (p.getImagens() == null ? List.<String>of() : p.getImagens()).stream().limit(2))
                    .toList());
                for (String foto : fotos == null ? List.<String>of() : fotos) {
                    String arquivo = foto.substring(foto.lastIndexOf('/') + 1);
                    for (int largura : List.of(360, 540)) {
                        miniatura(largura, arquivo);
                        Thread.sleep(30);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                log.info("Preparo das miniaturas interrompido: {}", e.toString());
            }
        }, "preparo-miniaturas");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    @GetMapping("/miniaturas/{largura}/{arquivo}")
    public ResponseEntity<Resource> miniatura(@PathVariable int largura, @PathVariable String arquivo) {
        if (!LARGURAS.contains(largura) || !NOME_SEGURO.matcher(arquivo).matches()) {
            return ResponseEntity.notFound().build();
        }
        Path original = originais.resolve(arquivo).normalize();
        if (!original.startsWith(originais) || !Files.isRegularFile(original)) {
            return ResponseEntity.notFound().build();
        }

        Path pronta = pastaMiniaturas.resolve(largura + "-" + arquivo.replaceAll("\\.\\w+$", "") + ".jpg");
        Path usarOriginal = pronta.resolveSibling(pronta.getFileName() + ".usar-original");
        try {
            if (Files.exists(usarOriginal)) return entregar(original);
            if (!Files.exists(pronta) && !gerar(original, pronta, largura)) return entregar(original);
            return entregar(pronta);
        } catch (Exception | OutOfMemoryError e) {
            log.warn("Miniatura indisponível para {} ({}): usando a original", arquivo, e.toString());
            return entregar(original);
        }
    }

    private ResponseEntity<Resource> entregar(Path arquivo) {
        MediaType tipo = MediaTypeFactory.getMediaType(arquivo.getFileName().toString()).orElse(MediaType.IMAGE_JPEG);
        return ResponseEntity.ok().cacheControl(TRINTA_DIAS).contentType(tipo).body(new FileSystemResource(arquivo));
    }

    /** Gera a miniatura; devolve false quando a original deve ser usada no lugar dela. */
    private boolean gerar(Path original, Path destino, int largura) throws IOException {
        if (orientacaoExif(original) > 1) return false; // foto girada por EXIF: o ImageIO a desenharia de lado
        if (!vagas.tryAcquire()) return false; // ocupado: a original sai na hora, sem fila
        try {
            BufferedImage imagem = lerReduzida(original, largura);
            if (imagem == null || imagem.getWidth() <= largura) return false; // já é pequena: a original serve
            int altura = Math.round(imagem.getHeight() * (largura / (float) imagem.getWidth()));

            BufferedImage reduzida = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = reduzida.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE); // PNG com transparência ganha fundo branco
            g.fillRect(0, 0, largura, altura);
            g.drawImage(imagem, 0, 0, largura, altura, null);
            g.dispose();

            Files.createDirectories(destino.getParent());
            Path temporario = Files.createTempFile(destino.getParent(), "gerando-", ".jpg");
            gravarJpeg(reduzida, temporario);
            if (Files.size(temporario) >= Files.size(original)) { // a original já é leve: não vale a pena
                Files.deleteIfExists(temporario);
                Files.createFile(destino.resolveSibling(destino.getFileName() + ".usar-original")); // lembra a decisão
                return false;
            }
            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return true;
        } finally {
            vagas.release();
        }
    }

    /** Decodifica já reduzida (subamostragem), sem carregar a foto inteira na memória. */
    private static BufferedImage lerReduzida(Path arquivo, int largura) throws IOException {
        try (ImageInputStream entrada = ImageIO.createImageInputStream(arquivo.toFile())) {
            Iterator<ImageReader> leitores = ImageIO.getImageReaders(entrada);
            if (!leitores.hasNext()) return null;
            ImageReader leitor = leitores.next();
            try {
                leitor.setInput(entrada, true, true);
                int passo = Math.max(1, leitor.getWidth(0) / (largura * 2));
                ImageReadParam parametros = leitor.getDefaultReadParam();
                parametros.setSourceSubsampling(passo, passo, 0, 0);
                return leitor.read(0, parametros);
            } finally {
                leitor.dispose();
            }
        }
    }

    private static void gravarJpeg(BufferedImage imagem, Path destino) throws IOException {
        ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (ImageOutputStream saida = ImageIO.createImageOutputStream(destino.toFile())) {
            escritor.setOutput(saida);
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(0.78f);
            parametros.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
            escritor.write(null, new IIOImage(imagem, null, null), parametros);
        } finally {
            escritor.dispose();
        }
    }

    /** Lê a tag Orientation (0x0112) do EXIF de um JPEG; 0 quando não houver. */
    static int orientacaoExif(Path arquivo) {
        try (InputStream in = Files.newInputStream(arquivo)) {
            byte[] dados = in.readNBytes(128 * 1024);
            if (dados.length < 4 || (dados[0] & 0xFF) != 0xFF || (dados[1] & 0xFF) != 0xD8) return 0; // não é JPEG
            int i = 2;
            while (i + 4 < dados.length && (dados[i] & 0xFF) == 0xFF) {
                int marcador = dados[i + 1] & 0xFF;
                int tamanho = ((dados[i + 2] & 0xFF) << 8) | (dados[i + 3] & 0xFF);
                if (marcador == 0xDA) return 0;
                if (marcador == 0xE1 && i + 10 < dados.length && new String(dados, i + 4, 4, "ISO-8859-1").equals("Exif")) {
                    int tiff = i + 10;
                    ByteBuffer b = ByteBuffer.wrap(dados, 0, Math.min(dados.length, i + 2 + tamanho));
                    b.order(dados[tiff] == 'I' ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
                    int ifd = tiff + b.getInt(tiff + 4);
                    int entradas = b.getShort(ifd) & 0xFFFF;
                    for (int k = 0; k < entradas; k++) {
                        int e = ifd + 2 + 12 * k;
                        if ((b.getShort(e) & 0xFFFF) == 0x0112) return b.getShort(e + 8) & 0xFFFF;
                    }
                    return 0;
                }
                i += 2 + tamanho;
            }
        } catch (IOException | RuntimeException e) {
            return 9; // não deu para ler: trata como "girada" e usa a original
        }
        return 0;
    }
}
