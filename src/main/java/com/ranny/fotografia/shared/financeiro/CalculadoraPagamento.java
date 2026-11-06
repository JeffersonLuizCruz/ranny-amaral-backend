package com.ranny.fotografia.shared.financeiro;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * PATTERN: Utility (pure functions) / Specification.
 *
 * <p>Os calculos de entrada, percentual e saldo sao usados por agenda e
 * contrato. Em vez de duplicar arredondamentos em dois services, a regra vive em
 * funcoes puras, sem estado e sem dependencia de Spring.
 *
 * <p>Beneficios: um unico arredondamento (HALF_UP, 2 casas) para toda a
 * aplicacao, testes triviais e nenhuma divergencia entre modulos.
 */
public final class CalculadoraPagamento {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private CalculadoraPagamento() {
    }

    public static BigDecimal normalizar(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal valorEntrada(BigDecimal valorTotal, BigDecimal percentual) {
        if (valorTotal == null || percentual == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal percentualSeguro = percentual.max(BigDecimal.ZERO).min(CEM);
        return valorTotal.multiply(percentualSeguro)
                .divide(CEM, 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal percentualEntrada(BigDecimal valorTotal, BigDecimal valorEntrada) {
        if (valorTotal == null || valorTotal.signum() <= 0 || valorEntrada == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return valorEntrada.multiply(CEM)
                .divide(valorTotal, 2, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO);
    }

    public static BigDecimal saldoRestante(BigDecimal valorTotal, BigDecimal valorEntrada) {
        BigDecimal total = normalizar(valorTotal);
        BigDecimal entrada = normalizar(valorEntrada);
        return total.subtract(entrada).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
