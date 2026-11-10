package com.ranny.fotografia.shared.model;

import java.math.BigDecimal;

/**
 * Snapshot imutavel de uma faixa de preco da foto extra.
 *
 * <p>O catalogo guarda as faixas em tabela propria; agenda, galeria e contrato
 * congelam esta representacao para que alteracoes futuras no pacote nao mudem
 * vendas ja fechadas.
 */
public record FaixaFotoExtraSnapshot(
        int quantidadeMinima,
        BigDecimal valorUnitario
) {
}
