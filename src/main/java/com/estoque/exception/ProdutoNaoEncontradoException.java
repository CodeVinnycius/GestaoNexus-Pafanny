package com.estoque.exception;

/** Lançada quando um produto não é encontrado no banco. */
public class ProdutoNaoEncontradoException extends RuntimeException {
    public ProdutoNaoEncontradoException(String nome) {
        super("Produto \"" + nome + "\" não encontrado no estoque.");
    }
    public ProdutoNaoEncontradoException(Long id) {
        super("Produto com id " + id + " não encontrado no estoque.");
    }
}
