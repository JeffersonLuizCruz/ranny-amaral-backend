package com.ranny.fotografia.contrato.api;

import com.ranny.fotografia.shared.model.FormaPagamentoSaldo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/** Payloads das acoes de pagamento do contrato. */
public final class PagamentoContratoRequests {

    private PagamentoContratoRequests() {
    }

    public record ConfirmacaoRequest(
            @Size(max = 120, message = "deve ter no maximo 120 caracteres")
            String confirmadoPor
    ) {
    }

    public record RejeicaoRequest(
            @NotBlank(message = "nao pode ser vazio")
            @Size(max = 500, message = "deve ter no maximo 500 caracteres")
            String motivo
    ) {
    }

    public record PagamentoFinalRequest(
            @PositiveOrZero(message = "deve ser maior ou igual a zero")
            BigDecimal valor,
            FormaPagamentoSaldo formaPagamento,
            Instant recebidoEm,
            @Size(max = 120, message = "deve ter no maximo 120 caracteres")
            String recebidoPor,
            @Size(max = 1000, message = "deve ter no maximo 1000 caracteres")
            String observacoes
    ) {
    }
}
