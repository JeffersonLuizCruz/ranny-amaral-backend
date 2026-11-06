package com.ranny.fotografia.financeiro.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** DTOs do read model financeiro mensal. */
public final class FinanceiroDtos {

    private FinanceiroDtos() {
    }

    public record ResumoMensalResponse(
            String mes,
            int quantidade,
            int quantidadeRecebidas,
            int quantidadePendentes,
            BigDecimal totalPrevisto,
            BigDecimal totalRecebido,
            BigDecimal totalAReceber,
            BigDecimal totalEntradasRecebidas,
            BigDecimal totalEntradasPendentes,
            BigDecimal totalSaldos,
            BigDecimal totalSaldosRecebidos,
            BigDecimal totalExtras,
            BigDecimal totalExtrasRecebidos,
            BigDecimal totalExtrasPendentes,
            int quantidadeSaldosPendentes,
            int quantidadeExtrasPendentes,
            BigDecimal ticketMedio,
            BigDecimal percentualRecebido,
            List<EntradaResponse> entradas
    ) {
    }

    public record EntradaResponse(
            String agendamentoId,
            String contratoId,
            String clienteId,
            String clienteNome,
            String pacoteId,
            String pacoteNome,
            OffsetDateTime dataHoraInicio,
            OffsetDateTime dataHoraFim,
            String statusAgendamento,
            BigDecimal valorTotal,
            BigDecimal valorEntrada,
            BigDecimal percentualEntrada,
            BigDecimal saldoRestante,
            String statusEntrada,
            String statusFinal,
            BigDecimal valorExtras,
            String statusExtras,
            BigDecimal recebidoEntrada,
            BigDecimal recebidoFinal,
            BigDecimal recebidoExtras,
            BigDecimal recebido,
            BigDecimal aReceber
    ) {
    }
}
