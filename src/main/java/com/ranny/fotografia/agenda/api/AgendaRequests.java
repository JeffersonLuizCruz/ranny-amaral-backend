package com.ranny.fotografia.agenda.api;

import com.ranny.fotografia.agenda.model.StatusAgendamento;
import com.ranny.fotografia.shared.model.FormaPagamentoSaldo;
import com.ranny.fotografia.shared.model.PagamentoStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/** Payloads de alteracao de status e pagamentos do agendamento. */
public final class AgendaRequests {

    private AgendaRequests() {
    }

    public record StatusRequest(
            @NotNull(message = "nao pode ser nulo")
            StatusAgendamento status
    ) {
    }

    public record PagamentoEntradaRequest(
            @PositiveOrZero(message = "deve ser maior ou igual a zero")
            BigDecimal valor,
            @PositiveOrZero(message = "deve ser maior ou igual a zero")
            BigDecimal percentual,
            PagamentoStatus status,
            @Size(max = 1000, message = "deve ter no maximo 1000 caracteres")
            String observacoes
    ) {
    }

    public record PagamentoFinalRequest(
            @PositiveOrZero(message = "deve ser maior ou igual a zero")
            BigDecimal valor,
            PagamentoStatus status,
            FormaPagamentoSaldo formaPagamento,
            Instant recebidoEm,
            @Size(max = 120, message = "deve ter no maximo 120 caracteres")
            String recebidoPor,
            @Size(max = 1000, message = "deve ter no maximo 1000 caracteres")
            String observacoes
    ) {
    }
}
