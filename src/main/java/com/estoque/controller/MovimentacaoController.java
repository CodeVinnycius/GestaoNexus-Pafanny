package com.estoque.controller;

import com.estoque.model.*;
import com.estoque.service.MovimentacaoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/movimentacoes")
public class MovimentacaoController {

    private final MovimentacaoService service;

    public MovimentacaoController(MovimentacaoService service) {
        this.service = service;
    }

    /** Histórico da empresa, do mais recente para o mais antigo. */
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

    /**
     * Cada problema no corpo tem a sua mensagem, porque ela aparece direto na tela. Erros do
     * service (estoque insuficiente, produto de outra empresa) seguem pelo GlobalExceptionHandler.
     */
    @PostMapping
    public ResponseEntity<?> registrar(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Empresa empresa) {
        Object produtoBruto = body.get("produtoId");
        Object tipoBruto = body.get("tipo");
        Object quantidadeBruta = body.get("quantidade");
        if (produtoBruto == null || tipoBruto == null || quantidadeBruta == null)
            return erro("Informe o produto, o tipo e a quantidade.");

        TipoMovimentacao tipo;
        try {
            tipo = TipoMovimentacao.valueOf(tipoBruto.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            return erro("Tipo inválido. Use: VENDA, ENTRADA, AJUSTE ou DEVOLUCAO.");
        }

        long produtoId;
        int quantidade;
        try {
            produtoId = Long.parseLong(produtoBruto.toString());
            quantidade = Integer.parseInt(quantidadeBruta.toString());
        } catch (NumberFormatException e) {
            return erro("Produto e quantidade precisam ser números inteiros.");
        }

        // Só o ajuste pode tirar do estoque com número negativo (ex.: peça danificada).
        if (tipo == TipoMovimentacao.AJUSTE && quantidade == 0)
            return erro("O ajuste não pode ser zero.");
        if (tipo != TipoMovimentacao.AJUSTE && quantidade <= 0)
            return erro("Quantidade deve ser maior que zero.");

        String responsavel = Objects.toString(body.get("responsavel"), "—");
        String motivo = Objects.toString(body.get("motivo"), "");
        return ResponseEntity.ok(service.registrar(produtoId, tipo, quantidade, responsavel, motivo, empresa));
    }

    private static ResponseEntity<Map<String, String>> erro(String mensagem) {
        return ResponseEntity.badRequest().body(Map.of("erro", mensagem));
    }

    @GetMapping("/receita")
    public ResponseEntity<Map<String, Double>> receita(@AuthenticationPrincipal Empresa empresa) {
        return ResponseEntity.ok(Map.of("receita", service.receitaTotalVendas(empresa)));
    }

    @GetMapping("/exportar/csv")
    public ResponseEntity<byte[]> exportarCsv(@AuthenticationPrincipal Empresa empresa) {
        byte[] corpo = com.estoque.util.CsvUtil.paraBytesComBom(service.exportarCsv(empresa));
        String nomeArquivo = "estoque-historico-" + java.time.LocalDate.now() + ".csv";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(corpo);
    }
}
