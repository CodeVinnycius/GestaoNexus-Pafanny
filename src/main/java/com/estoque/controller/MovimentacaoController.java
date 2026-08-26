package com.estoque.controller;

import com.estoque.model.*;
import com.estoque.service.MovimentacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/movimentacoes")
public class MovimentacaoController {

    private final MovimentacaoService service;

    public MovimentacaoController(MovimentacaoService service) {
        this.service = service;
    }

    /** Lista todo o histórico da empresa (mais recentes primeiro) */
    @GetMapping
    public ResponseEntity<List<Movimentacao>> listar(
            @RequestParam(required = false) String tipo,
            @AuthenticationPrincipal Empresa empresa) {

        if (tipo != null && !tipo.isBlank()) {
            try {
                TipoMovimentacao t = TipoMovimentacao.valueOf(tipo.toUpperCase());
                return ResponseEntity.ok(service.listarPorTipo(empresa, t));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok(service.listar(empresa));
    }

    /** Registra qualquer tipo de movimentação */
    @PostMapping
    public ResponseEntity<?> registrar(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Empresa empresa) {
        try {
            Long   produtoId   = Long.parseLong(body.get("produtoId").toString());
            String tipoStr     = body.get("tipo").toString();
            int    quantidade  = Integer.parseInt(body.get("quantidade").toString());
            String responsavel = (String) body.getOrDefault("responsavel", "—");
            String motivo      = (String) body.getOrDefault("motivo", "");

            if (quantidade <= 0)
                return ResponseEntity.badRequest().body(Map.of("erro", "Quantidade deve ser maior que zero."));

            TipoMovimentacao tipo = TipoMovimentacao.valueOf(tipoStr.toUpperCase());
            Movimentacao mov = service.registrar(produtoId, tipo, quantidade, responsavel, motivo, empresa);
            return ResponseEntity.ok(mov);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Tipo inválido. Use: VENDA, ENTRADA, AJUSTE ou DEVOLUCAO."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    /** Receita total de vendas */
    @GetMapping("/receita")
    public ResponseEntity<Map<String, Double>> receita(@AuthenticationPrincipal Empresa empresa) {
        return ResponseEntity.ok(Map.of("receita", service.receitaTotalVendas(empresa)));
    }
}
