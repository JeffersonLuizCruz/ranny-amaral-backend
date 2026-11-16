package com.ranny.fotografia.agenda.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Bloqueio pontual de agenda.
 *
 * <p>Com {@code diaInteiro=true}, {@code horaInicio}/{@code horaFim} sao
 * ignorados e o backend materializa o dia completo no fuso do estudio.
 */
public record BloqueioAgendaRequest(
        @NotNull(message = "nao pode ser nulo")
        LocalDate data,

        boolean diaInteiro,

        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "deve estar no formato HH:mm")
        String horaInicio,

        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "deve estar no formato HH:mm")
        String horaFim,

        @NotBlank(message = "nao pode ser vazio")
        @Size(max = 255, message = "deve ter no maximo 255 caracteres")
        String motivo
) {
}
