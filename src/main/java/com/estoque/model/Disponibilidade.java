package com.estoque.model;

/**
 * Status de disponibilidade exibido à cliente.
 * CONSULTAR é o padrão enquanto não há controle real de estoque/tamanho por peça.
 */
public enum Disponibilidade {
    CONSULTAR,          // ainda sem estoque/tamanho cadastrado — "Consulte pelo WhatsApp"
    DISPONIVEL,
    ULTIMAS_UNIDADES,
    INDISPONIVEL,
    ESGOTADO
}
