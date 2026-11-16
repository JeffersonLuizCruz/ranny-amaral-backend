package com.ranny.fotografia.agenda.api;

import com.ranny.fotografia.agenda.model.BloqueioAgenda;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** DTO de saida do bloqueio de agenda. */
public record BloqueioAgendaResponse(
        String id,
        OffsetDateTime inicio,
        OffsetDateTime fim,
        boolean diaInteiro,
        LocalDate data,
        String horaInicio,
        String horaFim,
        String motivo,
        Instant createdAt,
        Instant updatedAt
) {

    private static final ZoneId FUSO_ESTUDIO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    public static BloqueioAgendaResponse de(BloqueioAgenda bloqueio) {
        var inicio = bloqueio.getInicio().atZoneSameInstant(FUSO_ESTUDIO);
        var fim = bloqueio.getFim().atZoneSameInstant(FUSO_ESTUDIO);
        return new BloqueioAgendaResponse(
                bloqueio.getId(),
                bloqueio.getInicio(),
                bloqueio.getFim(),
                bloqueio.isDiaInteiro(),
                inicio.toLocalDate(),
                HORA.format(inicio),
                HORA.format(fim),
                bloqueio.getMotivo(),
                bloqueio.getAuditoria().getCriadoEm(),
                bloqueio.getAuditoria().getAtualizadoEm());
    }
}
