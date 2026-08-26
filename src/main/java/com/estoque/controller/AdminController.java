package com.estoque.controller;

import com.estoque.model.Empresa;
import com.estoque.service.EmpresaService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/empresas")
public class AdminController {

    private final EmpresaService service;

    public AdminController(EmpresaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Empresa>> listar() {
        return ResponseEntity.ok(service.listarTodas());
    }

    @PostMapping
    public ResponseEntity<?> cadastrar(@RequestBody Map<String, String> body) {
        try {
            String nome  = body.get("nome");
            String login = body.get("login");
            String senha = body.get("senha");

            if (nome == null || login == null || senha == null ||
                nome.isBlank() || login.isBlank() || senha.isBlank())
                return ResponseEntity.badRequest().body(Map.of("erro", "Nome, login e senha são obrigatórios."));

            Empresa e = service.cadastrar(nome.trim(), login.trim(), senha);
            return ResponseEntity.status(HttpStatus.CREATED).body(e);

        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(Map.of("erro", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id,
                                        @RequestBody Map<String, Object> body) {
        try {
            String  nome     = (String)  body.get("nome");
            String  login    = (String)  body.get("login");
            String  senha    = (String)  body.get("senha");
            Boolean ativo    = body.get("ativo") != null ? (Boolean) body.get("ativo") : null;

            Empresa e = service.atualizar(id, nome, login, senha, ativo);
            return ResponseEntity.ok(e);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> remover(@PathVariable Long id) {
        try {
            service.remover(id);
            return ResponseEntity.ok(Map.of("mensagem", "Empresa removida com sucesso."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }
}
