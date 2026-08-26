package com.estoque.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Salva fotos de produto em disco (./data/uploads/produtos), servidas em /uploads/**. */
@Service
public class ArmazenamentoImagemService {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long TAMANHO_MAX_BYTES = 5L * 1024 * 1024; // 5MB

    @Value("${app.uploads.dir:./data/uploads}")
    private String uploadsDir;

    public String salvar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty())
            throw new IllegalArgumentException("Nenhum arquivo enviado.");
        if (arquivo.getSize() > TAMANHO_MAX_BYTES)
            throw new IllegalArgumentException("Imagem maior que 5MB.");

        String contentType = arquivo.getContentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType))
            throw new IllegalArgumentException("Formato de imagem inválido. Use JPEG, PNG ou WebP.");

        String extensao = Map.of(
            "image/jpeg", ".jpg",
            "image/png",  ".png",
            "image/webp", ".webp"
        ).get(contentType);

        String nomeArquivo = UUID.randomUUID() + extensao;

        try {
            Path destino = Path.of(uploadsDir, "produtos").normalize();
            Files.createDirectories(destino);
            Path arquivoDestino = destino.resolve(nomeArquivo).normalize();
            if (!arquivoDestino.startsWith(destino))
                throw new IllegalArgumentException("Nome de arquivo inválido.");
            arquivo.transferTo(arquivoDestino);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao salvar imagem.", e);
        }

        return "/uploads/produtos/" + nomeArquivo;
    }

    /** Remove o arquivo físico referenciado por uma URL relativa (ex: /uploads/produtos/xxx.jpg). */
    public void remover(String urlRelativa) {
        if (urlRelativa == null || !StringUtils.hasText(urlRelativa)) return;
        String nome = Path.of(urlRelativa).getFileName().toString();
        Path destino = Path.of(uploadsDir, "produtos").normalize();
        Path arquivo = destino.resolve(nome).normalize();
        if (!arquivo.startsWith(destino)) return;
        try {
            Files.deleteIfExists(arquivo);
        } catch (IOException ignored) {
            // Se o arquivo não puder ser removido, a referência já foi tirada do produto — não é crítico.
        }
    }
}
