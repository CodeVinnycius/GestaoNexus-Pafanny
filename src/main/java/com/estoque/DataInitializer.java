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
    // Recuperação de acesso: com true, a senha do admin existente volta a ser app.admin.senha
    // a cada inicialização. Ligar só para recuperar o acesso e desligar logo depois.
    @Value("${app.admin.redefinir-senha:false}") private boolean redefinirSenha;

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
        } else if (redefinirSenha) {
            Empresa admin = repository.findByLogin(adminLogin).orElseThrow();
            admin.setSenha(encoder.encode(adminSenha));
            admin.setAdmin(true);
            admin.setAtivo(true);
            repository.save(admin);
            log.warn("Senha do admin '{}' redefinida por APP_ADMIN_REDEFINIR_SENHA. Desligue essa variável.", adminLogin);
        }
    }
}
