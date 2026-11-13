package com.ranny.fotografia.shared.financeiro;

import com.ranny.fotografia.shared.model.FaixaFotoExtraSnapshot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * Calculo de fotos extras por faixa de quantidade.
 *
 * <p>PATTERN: Volume Pricing. A faixa de maior quantidade minima alcancada
 * define o valor unitario de <strong>todas</strong> as fotos extras daquela
 * selecao. Ex.: base 15, faixa 3 -> 10; 2 extras = 2 x 15, 3 extras = 3 x 10.
 *
 * <p>Sem faixa configurada no pacote, vale apenas o valor base.
 */
public final class CalculadoraExtras {

    private CalculadoraExtras() {
    }

    public static ResultadoExtras calcular(BigDecimal valorBase,
                                            List<FaixaFotoExtraSnapshot> faixas,
                                            int quantidade) {
        if (quantidade <= 0) {
            return new ResultadoExtras(zero(), zero(), List.of());
        }

        BigDecimal unitario = precoUnitario(valorBase, faixas, quantidade);
        BigDecimal total = unitario.multiply(BigDecimal.valueOf(quantidade))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal medio = total.divide(
                BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);
        return new ResultadoExtras(total, medio,
                List.of(new FaixaAplicada(quantidade, unitario, total)));
    }

    /**
     * Calcula apenas o custo marginal das fotos que ainda nao foram cobertas.
     *
     * <p>O total e sempre recalculado com a faixa vigente; por isso uma
     * quantidade que atinge a faixa pode ter delta zero (as fotos anteriores
     * sao retroativamente ajustadas para o novo valor unitario).
     */
    public static ResultadoDeltaExtras calcularDelta(BigDecimal valorBase,
                                                     List<FaixaFotoExtraSnapshot> faixas,
                                                     int quantidadeTotal,
                                                     int quantidadeCoberta) {
        if (quantidadeTotal < 0 || quantidadeCoberta < 0
                || quantidadeCoberta > quantidadeTotal) {
            throw new IllegalArgumentException("Quantidade coberta invalida.");
        }

        ResultadoExtras total = calcular(valorBase, faixas, quantidadeTotal);
        if (quantidadeCoberta == 0) {
            return new ResultadoDeltaExtras(
                    quantidadeTotal, total.valorTotal(), total.detalhamento());
        }

        ResultadoExtras coberto = calcular(valorBase, faixas, quantidadeCoberta);
        int quantidadeDelta = quantidadeTotal - quantidadeCoberta;
        BigDecimal valorDelta = total.valorTotal().subtract(coberto.valorTotal())
                .setScale(2, RoundingMode.HALF_UP);

        if (quantidadeDelta == 0 || valorDelta.signum() <= 0) {
            return new ResultadoDeltaExtras(quantidadeDelta, zero(), List.of());
        }

        BigDecimal unitario = valorDelta.divide(
                BigDecimal.valueOf(quantidadeDelta), 2, RoundingMode.HALF_UP);
        return new ResultadoDeltaExtras(quantidadeDelta, valorDelta,
                List.of(new FaixaAplicada(quantidadeDelta, unitario, valorDelta)));
    }

    private static BigDecimal precoUnitario(BigDecimal valorBase,
                                             List<FaixaFotoExtraSnapshot> faixas,
                                             int quantidade) {
        BigDecimal unitario = valorBase == null ? BigDecimal.ZERO : valorBase;
        if (faixas == null) {
            return unitario;
        }

        List<FaixaFotoExtraSnapshot> ordenadas = faixas.stream()
                .filter(faixa -> faixa != null
                        && faixa.quantidadeMinima() > 0
                        && faixa.valorUnitario() != null)
                .sorted(Comparator.comparingInt(FaixaFotoExtraSnapshot::quantidadeMinima))
                .toList();

        for (FaixaFotoExtraSnapshot faixa : ordenadas) {
            if (quantidade >= faixa.quantidadeMinima()) {
                unitario = faixa.valorUnitario();
            }
        }
        return unitario;
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
