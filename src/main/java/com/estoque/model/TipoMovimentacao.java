package com.estoque.model;

public enum TipoMovimentacao {
    VENDA,      // saída por venda
    ENTRADA,    // recebimento de mercadoria
    AJUSTE,     // correção manual de estoque
    DEVOLUCAO   // devolução de cliente (entrada)
}
