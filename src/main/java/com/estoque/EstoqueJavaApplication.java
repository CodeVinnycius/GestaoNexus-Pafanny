package com.estoque;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
        SpringApplication.run(EstoqueJavaApplication.class, args);
    }
}
