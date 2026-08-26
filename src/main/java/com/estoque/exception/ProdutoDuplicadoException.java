package com.estoque.exception;

/** Lançada quando já existe um produto com o mesmo nome. */
public class ProdutoDuplicadoException extends RuntimeException {
    public ProdutoDuplicadoException(String nome) {
        super("Já existe um produto com o nome \"" + nome + "\".");
    }
}
