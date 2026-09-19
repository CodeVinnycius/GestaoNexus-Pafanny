package com.estoque.controller;

import com.estoque.security.LoginRateLimiter;
import com.estoque.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter rateLimiter;

    public AuthController(AuthService authService, LoginRateLimiter rateLimiter) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String login = body.get("login");
        String senha = body.get("senha");
        if (login == null || senha == null || login.isBlank() || senha.isBlank())
            return ResponseEntity.badRequest().body(Map.of("erro", "Login e senha são obrigatórios."));

        login = login.trim();
        String ip = request.getRemoteAddr();
        if (rateLimiter.bloqueado(ip, login))
            return ResponseEntity.status(429)
                .body(Map.of("erro", "Muitas tentativas. Aguarde alguns minutos e tente de novo."));

        try {
            Map<String, Object> resultado = authService.login(login, senha);
            rateLimiter.registrarSucesso(ip, login);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            rateLimiter.registrarFalha(ip, login);
            return ResponseEntity.status(401).body(Map.of("erro", e.getMessage()));
        }
    }
}
