package com.ranny.fotografia.shared.financeiro;

import java.math.BigDecimal;
import java.util.List;

/** Resultado do calculo de fotos extras por faixa de quantidade. */
public record ResultadoExtras(
        BigDecimal valorTotal,
        BigDecimal valorMedioUnitario,
        List<FaixaAplicada> detalhamento
) {

    public ResultadoExtras {
        detalhamento = detalhamento == null ? List.of() : List.copyOf(detalhamento);
    }
}
