package com.ranny.fotografia.agenda.api;

import com.ranny.fotografia.agenda.model.StatusAgendamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * PATTERN: DTO / Form Object.
 *
 * <p>{@code clienteId} e {@code contato} sao opcionais: a reserva pode existir
 * antes de o cliente preencher os dados no aceite do contrato. Quando ha um
 * cliente vinculado, o contato e derivado dele; sem cliente, o contato informado
 * (se houver) e preservado apenas como referencia da reserva.
 */
public record AgendamentoRequest(
        String clienteId,
        @Valid ContatoRequest contato,

        @NotBlank(message = "nao pode ser vazio")
        String pacoteId,

        @NotNull(message = "nao pode ser nulo")
        OffsetDateTime dataHoraInicio,

        OffsetDateTime dataHoraFim,

        @NotBlank(message = "nao pode ser vazio")
        @Size(max = 180, message = "deve ter no maximo 180 caracteres")
        String local,

        StatusAgendamento status,

        @Size(max = 1000, message = "deve ter no maximo 1000 caracteres")
        String observacoes,

        @PositiveOrZero(message = "deve ser maior ou igual a zero")
        BigDecimal valorAcordado,

        @PositiveOrZero(message = "deve ser maior ou igual a zero")
        BigDecimal valorEntrada,

        @PositiveOrZero(message = "deve ser maior ou igual a zero")
        BigDecimal percentualEntrada,

        @PositiveOrZero(message = "deve ser maior ou igual a zero")
        BigDecimal taxaDeslocamento,

        Boolean repassarTaxaDeslocamento,

        /**
         * Indica se a promoção do catálogo deve ser congelada nas condições.
         * Quando ausente, mantém o comportamento padrão (aplicar se vigente).
         */
        Boolean aplicarPromocao
) {

    public record ContatoRequest(
            @Size(max = 120, message = "deve ter no maximo 120 caracteres")
            String nome,
            @Size(max = 30, message = "deve ter no maximo 30 caracteres")
            String whatsapp
    ) {
    }
}
