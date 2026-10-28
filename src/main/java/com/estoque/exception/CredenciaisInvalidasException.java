package com.estoque.exception;

/**
 * Login recusado. A mensagem vai para a tela de login, então é sempre um texto nosso; uma
 * exceção específica evita que um erro qualquer (banco fora do ar, por exemplo) apareça ali
 * e conte como tentativa errada.
 */
public class CredenciaisInvalidasException extends RuntimeException {
    public CredenciaisInvalidasException(String mensagem) {
        super(mensagem);
    }
}
