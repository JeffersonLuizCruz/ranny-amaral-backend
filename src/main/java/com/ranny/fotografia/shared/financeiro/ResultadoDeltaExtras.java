package com.ranny.fotografia.shared.financeiro;

import java.math.BigDecimal;
import java.util.List;

/** Resultado do delta de um pedido adicional de fotos extras. */
public record ResultadoDeltaExtras(
        int quantidade,
        BigDecimal valorTotal,
        List<FaixaAplicada> detalhamento
) {

    public ResultadoDeltaExtras {
        detalhamento = detalhamento == null ? List.of() : List.copyOf(detalhamento);
    }
}
