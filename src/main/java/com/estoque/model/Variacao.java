package com.estoque.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/** Grade de estoque por tamanho/cor de um produto (ex: Pijama Floral - M - Azul - 5un). */
@Entity
@Table(name = "variacoes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"produto_id", "tamanho", "cor"}))
public class Variacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O tamanho não pode ser vazio.")
    @Size(max = 20, message = "O tamanho pode ter no máximo 20 caracteres.")
    @Column(nullable = false, length = 20)
    private String tamanho;

    @Size(max = 40, message = "A cor pode ter no máximo 40 caracteres.")
    @Column(length = 40)
    private String cor;

    @Min(value = 0, message = "A quantidade não pode ser negativa.")
    @Column(nullable = false)
    private int quantidade;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    public Variacao() {}

    public Long   getId()         { return id; }
    public String getTamanho()    { return tamanho; }
    public String getCor()        { return cor; }
    public int    getQuantidade() { return quantidade; }
    public Produto getProduto()   { return produto; }

    public void setTamanho(String t)    { this.tamanho = t; }
    public void setCor(String c)        { this.cor = c; }
    public void setQuantidade(int q)    { this.quantidade = q; }
    public void setProduto(Produto p)   { this.produto = p; }
}
