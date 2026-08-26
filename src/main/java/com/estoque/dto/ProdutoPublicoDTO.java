package com.estoque.dto;

import com.estoque.model.Disponibilidade;
import com.estoque.model.Produto;
import com.estoque.model.Variacao;

import java.util.List;

/**
 * Representação pública de um produto para a loja (/api/loja).
 * Expõe só o que o cliente final pode ver — NUNCA precoCusto, empresa ou ids internos sensíveis.
 */
public class ProdutoPublicoDTO {

    public record VariacaoPublicaDTO(Long id, String tamanho, String cor, int quantidade) {}

    private final Long id;
    private final String nome;
    private final String codigo;
    private final String descricao;
    private final String categoria;
    private final String estampa;
    /** Nulo = preço ainda não definido para esta peça ("Consulte o valor"), nunca inventado. */
    private final Double preco;
    private final Disponibilidade disponibilidade;
    private final List<String> imagens;
    private final List<VariacaoPublicaDTO> variacoes;

    public ProdutoPublicoDTO(Produto p) {
        this.id              = p.getId();
        this.nome            = p.getNome();
        this.codigo          = p.getCodigo();
        this.descricao       = p.getDescricao();
        this.categoria       = p.getCategoria();
        this.estampa         = p.getEstampa();
        this.preco           = p.getPrecoVenda();
        this.disponibilidade = p.getDisponibilidade();
        this.imagens         = p.getImagens();
        this.variacoes = p.getVariacoes().stream()
            .filter(v -> v.getQuantidade() > 0)
            .map(v -> new VariacaoPublicaDTO(v.getId(), v.getTamanho(), v.getCor(), v.getQuantidade()))
            .toList();
    }

    public Long getId()               { return id; }
    public String getNome()           { return nome; }
    public String getCodigo()         { return codigo; }
    public String getDescricao()      { return descricao; }
    public String getCategoria()      { return categoria; }
    public String getEstampa()        { return estampa; }
    public Double getPreco()          { return preco; }
    public Disponibilidade getDisponibilidade() { return disponibilidade; }
    public List<String> getImagens()  { return imagens; }
    public List<VariacaoPublicaDTO> getVariacoes() { return variacoes; }
}
