package com.estoque;

import com.estoque.service.RestauracaoService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Path;

@SpringBootApplication
public class EstoqueJavaApplication {

    public static void main(String[] args) {
        // Tem que ser aqui, antes do Spring: depois que o H2 abre o arquivo do banco, não dá mais
        // para trocá-lo. Se o admin enviou um backup para restaurar, a troca acontece agora.
        String dados = System.getenv().getOrDefault("APP_DATA_DIR", "./data");
        RestauracaoService.aplicarPendente(Path.of(dados));
        SpringApplication.run(EstoqueJavaApplication.class, args);
    }
}
