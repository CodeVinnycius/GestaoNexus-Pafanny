package com.estoque.service;

import com.estoque.model.Empresa;
import com.estoque.repository.EmpresaRepository;
import com.estoque.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    private final EmpresaRepository empresaRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder encoder;

    public AuthService(EmpresaRepository empresaRepository,
                       JwtUtil jwtUtil,
                       PasswordEncoder encoder) {
        this.empresaRepository = empresaRepository;
        this.jwtUtil           = jwtUtil;
        this.encoder           = encoder;
    }

    public Map<String, Object> login(String login, String senha) {
        Empresa empresa = empresaRepository.findByLogin(login)
            .orElseThrow(() -> new RuntimeException("Login ou senha incorretos."));

        if (!empresa.isAtivo())
            throw new RuntimeException("Empresa inativa. Entre em contato com o administrador.");

        if (!encoder.matches(senha, empresa.getSenha()))
            throw new RuntimeException("Login ou senha incorretos.");

        String token = jwtUtil.gerarToken(empresa.getId(), empresa.getLogin(), empresa.isAdmin());

        return Map.of(
            "token",  token,
            "nome",   empresa.getNome(),
            "login",  empresa.getLogin(),
            "admin",  empresa.isAdmin()
        );
    }
}
