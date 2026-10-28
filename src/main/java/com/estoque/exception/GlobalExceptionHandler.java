package com.estoque.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.*;

/**
 * Handler global — nunca expõe stack traces ou detalhes técnicos ao cliente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProdutoNaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> tratarProdutoNaoEncontrado(ProdutoNaoEncontradoException ex) {
        return erro(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ProdutoDuplicadoException.class)
    public ResponseEntity<Map<String, String>> tratarProdutoDuplicado(ProdutoDuplicadoException ex) {
        return erro(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratarValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors())
            campos.put(fe.getField(), fe.getDefaultMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("erro", "Dados inválidos.");
        body.put("campos", campos);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> tratarArgumentoInvalido(IllegalArgumentException ex) {
        return erro(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> tratarNaoEncontrado(NoSuchElementException ex) {
        return erro(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Recurso estático inexistente (ex: link de imagem quebrado) — 404 puro, sem logar como erro. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> tratarRecursoEstaticoAusente(NoResourceFoundException ex) {
        return erro(HttpStatus.NOT_FOUND, "Recurso não encontrado.");
    }

    /**
     * Catch-all: loga internamente mas retorna mensagem genérica.
     * Previne vazamento de stack traces e detalhes do banco.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> tratarErroInesperado(Exception ex) {
        log.error("Erro interno: {}", ex.getMessage(), ex);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno. Tente novamente.");
    }

    private ResponseEntity<Map<String, String>> erro(HttpStatus status, String msg) {
        return ResponseEntity.status(status).body(Map.of("erro", msg));
    }
}
