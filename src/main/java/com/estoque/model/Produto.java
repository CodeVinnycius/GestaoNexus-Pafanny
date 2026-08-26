package com.estoque.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "produtos",
       uniqueConstraints = @UniqueConstraint(columnNames = {"nome", "empresa_id"}))
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome não pode ser vazio.")
    @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres.")
    @Column(nullable = false, length = 100)
    private String nome;

    @Min(value = 0, message = "A quantidade não pode ser negativa.")
    @Column(nullable = false)
    private int quantidade;

    @Size(max = 1000, message = "A descrição pode ter no máximo 1000 caracteres.")
    @Column(length = 1000)
    private String descricao;

    @Size(max = 60, message = "A categoria pode ter no máximo 60 caracteres.")
    @Column(length = 60)
    private String categoria;

    /** Controla se o produto aparece na loja pública (/api/loja). */
    @Column(name = "visivel_loja", nullable = false)
    private boolean visivelLoja = false;

    /** Caminhos relativos das fotos (ex: /uploads/produtos/{uuid}.jpg), em ordem de exibição. */
    @ElementCollection
    @CollectionTable(name = "produto_imagens", joinColumns = @JoinColumn(name = "produto_id"))
    @OrderColumn(name = "ordem")
    @Column(name = "url", length = 300)
    private List<String> imagens = new ArrayList<>();

    /**
     * Grade de estoque por tamanho/cor. Quando presente, "quantidade" reflete a soma delas.
     * Serializado normalmente aqui (painel interno, autenticado) — o catálogo público usa
     * ProdutoPublicoDTO, que nunca reaproveita esta serialização.
     */
    @OneToMany(mappedBy = "produto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Variacao> variacoes = new ArrayList<>();

    /** Preço de custo unitário (quanto pago por unidade) — NUNCA exposto na loja pública. */
    @DecimalMin(value = "0.0", inclusive = true, message = "O custo não pode ser negativo.")
    @Column(name = "preco_custo", nullable = false)
    private double precoCusto;

    /**
     * Preço de venda unitário. Nulo = ainda não definido para este produto
     * (loja mostra "Consulte o valor" em vez de inventar um preço).
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "O preço de venda não pode ser negativo.")
    @Column(name = "preco_venda")
    private Double precoVenda;

    /** Estampa/print da peça (ex: "Ursinho Pooh Flor Margarida"), separado do nome comercial. */
    @Size(max = 120, message = "A estampa pode ter no máximo 120 caracteres.")
    @Column(length = 120)
    private String estampa;

    /**
     * Código permanente e legível do produto (ex: pafanny-camiseta-botao-pooh-001).
     * Não muda ao renomear o produto — serve para referência interna, WhatsApp e futuros relatórios.
     */
    @Size(max = 80)
    @Column(length = 80)
    private String codigo;

    /**
     * Status de disponibilidade mostrado à cliente. Independente da quantidade/variações —
     * assim a loja nunca precisa inventar estoque para exibir uma peça.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "disponibilidade", nullable = false, length = 30)
    private Disponibilidade disponibilidade = Disponibilidade.CONSULTAR;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    public Produto() {}

    // ── Getters ──────────────────────────────────────────────────────────────

    public Long   getId()          { return id; }
    public String getNome()        { return nome; }
    public int    getQuantidade()  { return quantidade; }
    public double getPrecoCusto()  { return precoCusto; }
    public Double getPrecoVenda()  { return precoVenda; }
    public Empresa getEmpresa()    { return empresa; }
    public String getDescricao()   { return descricao; }
    public String getCategoria()   { return categoria; }
    public boolean isVisivelLoja() { return visivelLoja; }
    public List<String> getImagens()   { return imagens; }
    public List<Variacao> getVariacoes() { return variacoes; }
    public String getEstampa()     { return estampa; }
    public String getCodigo()      { return codigo; }
    public Disponibilidade getDisponibilidade() { return disponibilidade; }

    /** Custo total do estoque deste produto */
    @Transient @JsonProperty("custoTotal")
    public double getCustoTotal()   { return quantidade * precoCusto; }

    /** Valor total de venda do estoque */
    @Transient @JsonProperty("valorTotal")
    public double getValorTotal()   { return quantidade * (precoVenda != null ? precoVenda : 0); }

    /** Lucro potencial se vender tudo */
    @Transient @JsonProperty("lucroPotencial")
    public double getLucroPotencial() { return ((precoVenda != null ? precoVenda : 0) - precoCusto) * quantidade; }

    /** Margem de lucro em % */
    @Transient @JsonProperty("margemLucro")
    public double getMargemLucro() {
        if (precoVenda == null || precoVenda == 0) return 0;
        return ((precoVenda - precoCusto) / precoVenda) * 100;
    }

    // Retrocompatibilidade: preco = precoVenda para o front existente
    @Transient @JsonProperty("preco")
    public Double getPreco() { return precoVenda; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setNome(String n)        { this.nome = n; }
    public void setQuantidade(int q)     { this.quantidade = q; }
    public void setPrecoCusto(double v)  { this.precoCusto = v; }
    public void setPrecoVenda(Double v)  { this.precoVenda = v; }
    public void setEmpresa(Empresa e)    { this.empresa = e; }
    public void setDescricao(String d)   { this.descricao = d; }
    public void setCategoria(String c)   { this.categoria = c; }
    public void setVisivelLoja(boolean v){ this.visivelLoja = v; }
    public void setImagens(List<String> i) { this.imagens = i; }
    public void setEstampa(String e)     { this.estampa = e; }
    public void setCodigo(String c)      { this.codigo = c; }
    public void setDisponibilidade(Disponibilidade d) { this.disponibilidade = d != null ? d : Disponibilidade.CONSULTAR; }
}
