package com.estoque.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

@Entity
@Table(name = "empresas")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome da empresa não pode ser vazio.")
    @Column(nullable = false, length = 100)
    private String nome;

    @NotBlank(message = "O login não pode ser vazio.")
    @Column(nullable = false, unique = true, length = 50)
    private String login;

    @JsonIgnore
    @Column(nullable = false)
    private String senha;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(nullable = false)
    private boolean admin = false;

    @JsonIgnore
    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Produto> produtos;

    public Empresa() {}

    public Empresa(String nome, String login, String senha) {
        this.nome  = nome;
        this.login = login;
        this.senha = senha;
    }

    public Long getId()           { return id; }
    public String getNome()       { return nome; }
    public String getLogin()      { return login; }
    public String getSenha()      { return senha; }
    public boolean isAtivo()      { return ativo; }
    public boolean isAdmin()      { return admin; }
    public List<Produto> getProdutos() { return produtos; }

    public void setNome(String nome)    { this.nome = nome; }
    public void setLogin(String login)  { this.login = login; }
    public void setSenha(String senha)  { this.senha = senha; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public void setAdmin(boolean admin) { this.admin = admin; }
}
