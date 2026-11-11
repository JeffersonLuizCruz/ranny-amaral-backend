package com.ranny.fotografia.shared.financeiro;

import com.ranny.fotografia.shared.model.FaixaFotoExtraSnapshot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculo progressivo das fotos extras.
 *
 * <p>A regra e marginal: o preco base vale ate a primeira faixa; cada faixa
 * vale do seu minimo ate o minimo da proxima (ou ate o fim). Ex.: base 15,
 * faixa 11 -> 10; 15 fotos = 10 x 15 + 5 x 10.
 */
public final class CalculadoraExtras {

    private CalculadoraExtras() {
    }

    public static ResultadoExtras calcular(BigDecimal valorBase,
                                           List<FaixaFotoExtraSnapshot> faixas,
                                           int quantidade) {
        BigDecimal base = valorBase == null ? BigDecimal.ZERO : valorBase;
        if (quantidade <= 0) {
            return new ResultadoExtras(zero(), zero(), List.of());
        }

        List<FaixaFotoExtraSnapshot> ordenadas = faixas == null ? List.of()
                : faixas.stream()
                        .filter(faixa -> faixa != null
                                && faixa.quantidadeMinima() > 0
                                && faixa.valorUnitario() != null)
                        .sorted(Comparator.comparingInt(FaixaFotoExtraSnapshot::quantidadeMinima))
                        .toList();

        List<FaixaAplicada> detalhamento = new ArrayList<>();
        BigDecimal total = zero();
        int cursor = 1;

        for (int i = 0; i < ordenadas.size() && cursor <= quantidade; i++) {
            FaixaFotoExtraSnapshot faixa = ordenadas.get(i);

            if (faixa.quantidadeMinima() > cursor) {
                int fimBase = Math.min(quantidade, faixa.quantidadeMinima() - 1);
                if (fimBase >= cursor) {
                    total = total.add(registrar(detalhamento, fimBase - cursor + 1, base));
                    cursor = fimBase + 1;
                }
            }

            if (cursor > quantidade) {
                break;
            }

            int proximoMinimo = i + 1 < ordenadas.size()
                    ? ordenadas.get(i + 1).quantidadeMinima()
                    : Integer.MAX_VALUE;
            int fimFaixa = Math.min(quantidade, proximoMinimo - 1);
            if (fimFaixa >= cursor) {
                total = total.add(registrar(detalhamento,
                        fimFaixa - cursor + 1, faixa.valorUnitario()));
                cursor = fimFaixa + 1;
            }
        }

        if (cursor <= quantidade) {
            total = total.add(registrar(detalhamento, quantidade - cursor + 1, base));
        }

        BigDecimal valorTotal = total.setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorMedio = valorTotal.divide(
                BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);
        return new ResultadoExtras(valorTotal, valorMedio, detalhamento);
    }

    private static BigDecimal registrar(List<FaixaAplicada> detalhamento,
                                        int quantidade,
                                        BigDecimal valorUnitario) {
        BigDecimal unitario = valorUnitario == null ? BigDecimal.ZERO : valorUnitario;
        BigDecimal subtotal = unitario.multiply(BigDecimal.valueOf(quantidade))
                .setScale(2, RoundingMode.HALF_UP);
        detalhamento.add(new FaixaAplicada(quantidade, unitario, subtotal));
        return subtotal;
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula apenas o custo marginal das fotos que ainda nao foram cobertas.
     *
     * <p>PATTERN: Progressive Pricing + Delta. A quantidade coberta representa
     * fotos ja pagas ou isentas; o detalhamento devolvido cobre somente as
     * posicoes {@code quantidadeCoberta + 1} ate {@code quantidadeTotal}.
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
        BigDecimal valorDelta = total.valorTotal().subtract(coberto.valorTotal());
        return new ResultadoDeltaExtras(
                quantidadeTotal - quantidadeCoberta,
                valorDelta.setScale(2, RoundingMode.HALF_UP),
                subtrair(total.detalhamento(), coberto.detalhamento()));
    }

    private static List<FaixaAplicada> subtrair(List<FaixaAplicada> total,
                                                List<FaixaAplicada> coberto) {
        Map<BigDecimal, Integer> restante = new LinkedHashMap<>();
        for (FaixaAplicada faixa : total) {
            BigDecimal chave = faixa.valorUnitario().setScale(2, RoundingMode.HALF_UP);
            restante.merge(chave, faixa.quantidade(), Integer::sum);
        }
        for (FaixaAplicada faixa : coberto) {
            BigDecimal chave = faixa.valorUnitario().setScale(2, RoundingMode.HALF_UP);
            restante.computeIfPresent(chave,
                    (ignorado, quantidade) -> quantidade - faixa.quantidade());
        }

        List<FaixaAplicada> delta = new ArrayList<>();
        for (Map.Entry<BigDecimal, Integer> entry : restante.entrySet()) {
            if (entry.getValue() <= 0) {
                continue;
            }
            BigDecimal subtotal = entry.getKey()
                    .multiply(BigDecimal.valueOf(entry.getValue()))
                    .setScale(2, RoundingMode.HALF_UP);
            delta.add(new FaixaAplicada(entry.getValue(), entry.getKey(), subtotal));
        }
        return delta;
    }
}
