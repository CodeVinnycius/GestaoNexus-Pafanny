package com.estoque.controller;

import com.estoque.model.Empresa;
import com.estoque.model.Produto;
import com.estoque.model.Variacao;
import com.estoque.service.ArmazenamentoImagemService;
import com.estoque.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService service;
    private final ArmazenamentoImagemService armazenamentoImagemService;

    public ProdutoController(ProdutoService service, ArmazenamentoImagemService armazenamentoImagemService) {
        this.service = service;
        this.armazenamentoImagemService = armazenamentoImagemService;
    }

    @GetMapping
    public ResponseEntity<List<Produto>> listar(
            @RequestParam(required = false) String busca,
            @AuthenticationPrincipal Empresa empresa) {
        List<Produto> lista = (busca != null && !busca.isBlank())
            ? service.buscarPorNome(busca, empresa)
            : service.listarTodos(empresa);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Produto> buscarPorId(@PathVariable Long id,
                                                @AuthenticationPrincipal Empresa empresa) {
        return ResponseEntity.ok(service.buscarPorId(id, empresa));
    }

    @GetMapping("/valor-total")
    public ResponseEntity<Map<String, Double>> valorTotal(@AuthenticationPrincipal Empresa empresa) {
        Map<String, Double> resumo = service.resumoFinanceiro(empresa);
        return ResponseEntity.ok(resumo);
    }

    @PostMapping
    public ResponseEntity<Produto> cadastrar(@Valid @RequestBody Produto produto,
                                              @AuthenticationPrincipal Empresa empresa) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.cadastrar(produto, empresa));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Produto> editar(@PathVariable Long id,
                                           @RequestBody Produto dados,
                                           @AuthenticationPrincipal Empresa empresa) {
        return ResponseEntity.ok(service.editar(id, dados, empresa));
    }

    @PatchMapping("/{id}/qtd")
    public ResponseEntity<?> atualizarQuantidade(@PathVariable Long id,
                                                   @RequestBody Map<String, Integer> body,
                                                   @AuthenticationPrincipal Empresa empresa) {
        Integer qtd = body.get("quantidade");
        if (qtd == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(service.atualizarQuantidade(id, qtd, empresa));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> remover(@PathVariable Long id,
                                                        @AuthenticationPrincipal Empresa empresa) {
        service.remover(id, empresa);
        return ResponseEntity.ok(Map.of("mensagem", "Produto removido com sucesso."));
    }

    // ── Variações (tamanho / cor) ───────────────────────────────────────────────

    @PostMapping("/{id}/variacoes")
    public ResponseEntity<Produto> adicionarVariacao(@PathVariable Long id,
                                                       @RequestBody Variacao dados,
                                                       @AuthenticationPrincipal Empresa empresa) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.adicionarVariacao(id, dados, empresa));
    }

    @PutMapping("/{id}/variacoes/{variacaoId}")
    public ResponseEntity<Produto> editarVariacao(@PathVariable Long id,
                                                    @PathVariable Long variacaoId,
                                                    @RequestBody Variacao dados,
                                                    @AuthenticationPrincipal Empresa empresa) {
        return ResponseEntity.ok(service.editarVariacao(id, variacaoId, dados, empresa));
    }

    @DeleteMapping("/{id}/variacoes/{variacaoId}")
    public ResponseEntity<Produto> removerVariacao(@PathVariable Long id,
                                                     @PathVariable Long variacaoId,
                                                     @AuthenticationPrincipal Empresa empresa) {
        return ResponseEntity.ok(service.removerVariacao(id, variacaoId, empresa));
    }

    // ── Imagens ──────────────────────────────────────────────────────────────

    @PostMapping(value = "/{id}/imagens", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Produto> enviarImagem(@PathVariable Long id,
                                                  @RequestParam("arquivo") MultipartFile arquivo,
                                                  @AuthenticationPrincipal Empresa empresa) {
        // Garante que o produto existe e pertence à empresa antes de gravar em disco.
        service.buscarPorId(id, empresa);
        String url = armazenamentoImagemService.salvar(arquivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.adicionarImagem(id, url, empresa));
    }

    @DeleteMapping("/{id}/imagens")
    public ResponseEntity<Produto> removerImagem(@PathVariable Long id,
                                                   @RequestParam String url,
                                                   @AuthenticationPrincipal Empresa empresa) {
        Produto p = service.removerImagem(id, url, empresa);
        armazenamentoImagemService.remover(url);
        return ResponseEntity.ok(p);
    }
}
