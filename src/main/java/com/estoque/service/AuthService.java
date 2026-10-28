package com.estoque.service;

import com.estoque.exception.CredenciaisInvalidasException;
import com.estoque.model.Empresa;
import com.estoque.repository.EmpresaRepository;
import com.estoque.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    private static final String SENHA_ERRADA = "Login ou senha incorretos.";

    private final EmpresaRepository empresaRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder encoder;
    /** Hash de uma senha qualquer, comparado quando o login não existe (ver login()). */
    private final String hashFicticio;

    public AuthService(EmpresaRepository empresaRepository,
                       JwtUtil jwtUtil,
                       PasswordEncoder encoder) {
        this.empresaRepository = empresaRepository;
        this.jwtUtil           = jwtUtil;
        this.encoder           = encoder;
        this.hashFicticio      = encoder.encode("login-que-nao-existe");
    }

    public Map<String, Object> login(String login, String senha) {
        Empresa empresa = empresaRepository.findByLogin(login).orElse(null);
        if (empresa == null) {
            // Gasta o mesmo tempo de um BCrypt de verdade: sem isso, a resposta mais rápida
            // denunciaria quais logins existem.
            encoder.matches(senha, hashFicticio);
            throw new CredenciaisInvalidasException(SENHA_ERRADA);
        }

        // A senha vem antes do "inativa", para esse aviso só aparecer a quem já sabe a senha.
        if (!encoder.matches(senha, empresa.getSenha()))
            throw new CredenciaisInvalidasException(SENHA_ERRADA);

        if (!empresa.isAtivo())
            throw new CredenciaisInvalidasException("Empresa inativa. Entre em contato com o administrador.");

        String token = jwtUtil.gerarToken(empresa.getId(), empresa.getLogin(), empresa.isAdmin());

        return Map.of(
            "token",  token,
            "nome",   empresa.getNome(),
            "login",  empresa.getLogin(),
            "admin",  empresa.isAdmin()
        );
    }
}
