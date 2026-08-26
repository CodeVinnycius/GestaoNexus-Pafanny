package com.estoque.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.uploads.dir:./data/uploads}")
    private String uploadsDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Path.of(uploadsDir).toAbsolutePath().normalize();
        try {
            // Precisa existir ANTES de gerar a URI: Path#toUri() só termina em "/"
            // (indicando diretório) quando o caminho já existe no disco.
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao criar diretório de uploads.", e);
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(dir.toUri().toString());
    }
}
