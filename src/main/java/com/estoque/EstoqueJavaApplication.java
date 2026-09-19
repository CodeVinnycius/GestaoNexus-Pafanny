package com.estoque;

import com.estoque.service.RestauracaoService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Path;

/**
 * Ponto de entrada da aplicação Spring Boot.
 *
 * Para executar:
 *   mvn spring-boot:run
 * ou gere o jar:
 *   mvn clean package
 *   java -jar target/estoque-java-1.0.0.jar
 */
@SpringBootApplication
public class EstoqueJavaApplication {

    public static void main(String[] args) {
        // Se o admin enviou um backup para restaurar, troca banco/fotos ANTES do H2 abrir o arquivo.
        String dados = System.getenv().getOrDefault("APP_DATA_DIR", "./data");
        RestauracaoService.aplicarPendente(Path.of(dados));
        SpringApplication.run(EstoqueJavaApplication.class, args);
    }
}
