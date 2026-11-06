package com.ranny.fotografia.financeiro;

import java.math.BigDecimal;

/**
 * PATTERN: Public API / Port do modulo {@code financeiro}.
 *
 * <p>O dashboard consome apenas os indicadores do mes, sem acessar o service
 * concreto nem os DTOs internos do modulo.
 */
public interface FinanceiroConsulta {

    ResumoDashboard resumoParaDashboard(String mes);

    record ResumoDashboard(
            String mes,
            BigDecimal totalPrevisto,
            BigDecimal totalRecebido,
            BigDecimal totalAReceber,
            int quantidadePendentes,
            int quantidadeExtrasPendentes
    ) {
    }
}
