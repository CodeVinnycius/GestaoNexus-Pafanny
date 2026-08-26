package com.estoque.controller;

import com.estoque.dto.ProdutoPublicoDTO;
import com.estoque.service.LojaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Catálogo público da loja — sem autenticação, consumido por /loja.html. */
@RestController
@RequestMapping("/api/loja")
public class LojaController {

    private final LojaService service;

    public LojaController(LojaService service) {
        this.service = service;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> config() {
        return ResponseEntity.ok(service.config());
    }

    @GetMapping("/produtos")
    public ResponseEntity<List<ProdutoPublicoDTO>> listar() {
        return ResponseEntity.ok(service.listarVisiveis());
    }

    @GetMapping("/produtos/{id}")
    public ResponseEntity<ProdutoPublicoDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarVisivelPorId(id));
    }
}
