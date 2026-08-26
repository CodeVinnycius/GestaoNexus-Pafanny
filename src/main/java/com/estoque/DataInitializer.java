package com.estoque;

import com.estoque.model.Empresa;
import com.estoque.repository.EmpresaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final EmpresaRepository repository;
    private final PasswordEncoder encoder;

    @Value("${app.admin.login}") private String adminLogin;
    @Value("${app.admin.senha}") private String adminSenha;

    public DataInitializer(EmpresaRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder    = encoder;
    }

    @Override
    public void run(String... args) {
        if (!repository.existsByLogin(adminLogin)) {
            Empresa admin = new Empresa("Administrador", adminLogin, encoder.encode(adminSenha));
            admin.setAdmin(true);
            repository.save(admin);
            // SEGURANÇA: nunca loga a senha em produção
            log.info("Admin criado com login: {}", adminLogin);
        }
    }
}
