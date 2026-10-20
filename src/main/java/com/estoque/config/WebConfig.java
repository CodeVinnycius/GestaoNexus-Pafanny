package com.estoque.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.uploads.dir:./data/uploads}")
    private String uploadsDir;

    // A raiz do domínio (/) e /loja.html são servidas pelo LojaPaginaController;
    // o sistema continua em /login.html e /index.html.

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
        // Fotos de produto têm nome único (UUID) e nunca mudam: podem ficar 30 dias no navegador.
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(dir.toUri().toString())
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());

        // Arquivos da loja: o LojaPaginaController acrescenta ?v=<hash do conteúdo> em cada link,
        // então o navegador pode guardá-los por 1 ano e mesmo assim recebe a versão nova a cada deploy.
        // (Os arquivos do sistema — style.css, script.js — seguem sem cache, como antes.)
        // Os padrões precisam de curinga: num padrão exato o Spring não acha o arquivo (404).
        CacheControl umAno = CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic();
        registry.addResourceHandler("/css/loja*.css", "/css/efeitos*.css")
                .addResourceLocations("classpath:/static/css/").setCacheControl(umAno);
        registry.addResourceHandler("/js/loja*.js", "/js/efeitos*.js")
                .addResourceLocations("classpath:/static/js/").setCacheControl(umAno);
        registry.addResourceHandler("/js/vendor/**")
                .addResourceLocations("classpath:/static/js/vendor/").setCacheControl(umAno);
        registry.addResourceHandler("/fonts/**")
                .addResourceLocations("classpath:/static/fonts/").setCacheControl(umAno);
        registry.addResourceHandler("/img/**")
                .addResourceLocations("classpath:/static/img/").setCacheControl(umAno);
    }
}
