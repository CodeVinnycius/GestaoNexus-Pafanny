package com.estoque.controller;

import com.estoque.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            String login = body.get("login");
            String senha = body.get("senha");

            if (login == null || senha == null || login.isBlank() || senha.isBlank())
                return ResponseEntity.badRequest().body(Map.of("erro", "Login e senha são obrigatórios."));

            Map<String, Object> resultado = authService.login(login.trim(), senha);
            return ResponseEntity.ok(resultado);

        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("erro", e.getMessage()));
        }
    }
}
