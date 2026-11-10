package com.ranny.fotografia.shared.financeiro;

import java.math.BigDecimal;

/** Trecho de uma cobranca progressiva: quantidade x valor unitario. */
public record FaixaAplicada(
        int quantidade,
        BigDecimal valorUnitario,
        BigDecimal subtotal
) {
}
