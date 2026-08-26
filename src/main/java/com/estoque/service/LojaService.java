package com.estoque.service;

import com.estoque.dto.ProdutoPublicoDTO;
import com.estoque.model.Empresa;
import com.estoque.model.Produto;
import com.estoque.repository.EmpresaRepository;
import com.estoque.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/** Catálogo público da loja — nenhum método aqui exige autenticação. */
@Service
@Transactional(readOnly = true)
public class LojaService {

    private final ProdutoRepository produtoRepository;
    private final EmpresaRepository empresaRepository;

    @Value("${app.loja.empresa-login}")
    private String lojaEmpresaLogin;

    @Value("${app.loja.whatsapp}")
    private String lojaWhatsapp;

    @Value("${app.loja.instagram}")
    private String lojaInstagram;

    public LojaService(ProdutoRepository produtoRepository, EmpresaRepository empresaRepository) {
        this.produtoRepository = produtoRepository;
        this.empresaRepository = empresaRepository;
    }

    public java.util.Map<String, String> config() {
        Empresa empresa = empresaDaLoja();
        return java.util.Map.of("nome", empresa.getNome(), "whatsapp", lojaWhatsapp, "instagram", lojaInstagram);
    }

    private Empresa empresaDaLoja() {
        return empresaRepository.findByLogin(lojaEmpresaLogin)
            .orElseThrow(() -> new NoSuchElementException(
                "Empresa da loja (\"" + lojaEmpresaLogin + "\") não está configurada."));
    }

    /**
     * Visível quando publicada. Produtos SEM grade de tamanho (ainda não temos estoque real
     * cadastrado) ficam visíveis independente da quantidade — a disponibilidade real é tratada
     * pelo campo "disponibilidade" ("Consulte pelo WhatsApp", etc), nunca inventada. Produtos COM
     * grade de tamanho (estoque real) somem quando todas as variações zeram, como antes.
     */
    private boolean elegivelParaLoja(Produto p) {
        return p.isVisivelLoja() && (p.getVariacoes().isEmpty() || p.getQuantidade() > 0);
    }

    public List<ProdutoPublicoDTO> listarVisiveis() {
        Empresa empresa = empresaDaLoja();
        return produtoRepository.findByEmpresa(empresa).stream()
            .filter(this::elegivelParaLoja)
            .map(ProdutoPublicoDTO::new)
            .toList();
    }

    public ProdutoPublicoDTO buscarVisivelPorId(Long id) {
        Empresa empresa = empresaDaLoja();
        Produto p = produtoRepository.findByIdAndEmpresa(id, empresa)
            .filter(this::elegivelParaLoja)
            .orElseThrow(() -> new NoSuchElementException("Produto não encontrado na loja."));
        return new ProdutoPublicoDTO(p);
    }
}
