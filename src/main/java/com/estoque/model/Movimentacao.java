package com.estoque.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "movimentacoes")
public class Movimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimentacao tipo;

    /** Positivo = entrada, negativo = saída */
    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false, length = 100)
    private String nomeProduto;   // snapshot: produto pode ser removido depois

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(length = 100)
    private String responsavel;

    @Column(length = 255)
    private String motivo;

    /** Preço unitário no momento da movimentação */
    @Column(name = "preco_unitario")
    private double precoUnitario;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    public Movimentacao() {}

    // ── Getters ──────────────────────────────────────────────────────

    public Long   getId()           { return id; }
    public TipoMovimentacao getTipo() { return tipo; }
    public int    getQuantidade()   { return quantidade; }
    public String getNomeProduto()  { return nomeProduto; }
    public LocalDateTime getDataHora() { return dataHora; }
    public String getResponsavel()  { return responsavel; }
    public String getMotivo()       { return motivo; }
    public double getPrecoUnitario(){ return precoUnitario; }
    public Empresa getEmpresa()     { return empresa; }
    public Produto getProduto()     { return produto; }

    public String getDataFormatada() {
        if (dataHora == null) return "";
        return dataHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public double getValorTotal() { return Math.abs(quantidade) * precoUnitario; }

    // ── Setters ──────────────────────────────────────────────────────

    public void setTipo(TipoMovimentacao t)      { this.tipo = t; }
    public void setQuantidade(int q)             { this.quantidade = q; }
    public void setNomeProduto(String n)         { this.nomeProduto = n; }
    public void setDataHora(LocalDateTime d)     { this.dataHora = d; }
    public void setResponsavel(String r)         { this.responsavel = r; }
    public void setMotivo(String m)              { this.motivo = m; }
    public void setPrecoUnitario(double p)       { this.precoUnitario = p; }
    public void setEmpresa(Empresa e)            { this.empresa = e; }
    public void setProduto(Produto p)            { this.produto = p; }
}
