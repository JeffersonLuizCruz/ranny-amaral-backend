package com.ranny.fotografia.shared.financeiro;

import com.ranny.fotografia.shared.model.PagamentoEntrada;
import com.ranny.fotografia.shared.model.PagamentoSaldo;
import com.ranny.fotografia.shared.model.PagamentoStatus;

import java.math.BigDecimal;

/**
 * Resumo financeiro comum de entrada + saldo final.
 *
 * <p>Contrato e agendamento usam os mesmos dois value objects. Este record
 * centraliza os campos derivados para que as respostas da API e o gate de
 * galeria nao repliquem regras diferentes.
 */
public record ResumoPagamento(
        PagamentoStatus statusEntrada,
        PagamentoStatus statusSaldoFinal,
        boolean entradaQuitada,
        boolean saldoQuitado,
        boolean quitado,
        BigDecimal valorRecebido,
        BigDecimal valorPendente
) {

    public static ResumoPagamento de(PagamentoEntrada entrada, PagamentoSaldo saldo,
                                     BigDecimal valorEntradaPrevisto, BigDecimal saldoPrevisto) {
        PagamentoStatus statusEntrada = entrada == null ? null : entrada.getStatus();
        PagamentoStatus statusSaldoFinal = saldo == null ? null : saldo.getStatus();

        BigDecimal entradaEsperada = nz(valorEntradaPrevisto);
        BigDecimal saldoEsperado = nz(saldoPrevisto);
        BigDecimal entradaRecebida = statusEntrada == PagamentoStatus.CONFIRMADO
                ? nz(entrada.getValor())
                : BigDecimal.ZERO;
        BigDecimal saldoRecebido = statusSaldoFinal == PagamentoStatus.CONFIRMADO
                ? nz(saldo.getValor())
                : BigDecimal.ZERO;

        boolean entradaQuitada = statusEntrada == PagamentoStatus.ISENTO
                || (statusEntrada == PagamentoStatus.CONFIRMADO
                && entradaRecebida.compareTo(entradaEsperada) >= 0);
        boolean saldoQuitado = statusSaldoFinal == PagamentoStatus.ISENTO
                || (statusSaldoFinal == PagamentoStatus.CONFIRMADO
                && saldoRecebido.compareTo(saldoEsperado) >= 0);

        BigDecimal entradaPendente = pendente(statusEntrada, entradaEsperada, entradaRecebida, statusEntrada == null);
        BigDecimal saldoPendente = pendente(statusSaldoFinal, saldoEsperado, saldoRecebido, statusSaldoFinal == null);
        BigDecimal valorRecebido = entradaRecebida.add(saldoRecebido);
        BigDecimal valorPendente = entradaPendente.add(saldoPendente);

        return new ResumoPagamento(
                statusEntrada,
                statusSaldoFinal,
                entradaQuitada,
                saldoQuitado,
                entradaQuitada && saldoQuitado,
                valorRecebido,
                valorPendente
        );
    }

    private static BigDecimal pendente(PagamentoStatus status, BigDecimal esperado,
                                       BigDecimal recebido, boolean statusAusente) {
        if (status == PagamentoStatus.PENDENTE || statusAusente) {
            return esperado;
        }
        if (status == PagamentoStatus.CONFIRMADO) {
            return esperado.subtract(recebido).max(BigDecimal.ZERO);
        }
        return BigDecimal.ZERO;
    }

    private static BigDecimal nz(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
