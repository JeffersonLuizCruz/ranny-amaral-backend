package com.ranny.fotografia.agenda.model;

import com.ranny.fotografia.shared.exception.RegraDeNegocioException;
import com.ranny.fotografia.shared.model.Auditoria;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Indisponibilidade pontual da agenda.
 *
 * <p>Um bloqueio de dia inteiro e materializado como 00:00 do dia ate 00:00 do
 * dia seguinte em {@code America/Sao_Paulo}; um bloqueio de intervalo usa os
 * horarios informados. A sobreposicao e sempre semiaberta
 * ({@code [inicio, fim)}), portanto um ensaio que termina exatamente quando o
 * bloqueio começa continua permitido.
 */
@Entity
@Table(name = "agenda_bloqueios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BloqueioAgenda {

    @Id
    @Column(length = 64)
    private String id;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(name = "data_hora_inicio", nullable = false)
    private OffsetDateTime inicio;

    @Column(name = "data_hora_fim", nullable = false)
    private OffsetDateTime fim;

    @Column(name = "dia_inteiro", nullable = false)
    private boolean diaInteiro;

    @Column(nullable = false, length = 255)
    private String motivo;

    @Embedded
    private Auditoria auditoria;

    public static BloqueioAgenda criar(OffsetDateTime inicio, OffsetDateTime fim,
                                       boolean diaInteiro, String motivo) {
        BloqueioAgenda bloqueio = new BloqueioAgenda();
        bloqueio.id = UUID.randomUUID().toString();
        bloqueio.auditoria = Auditoria.iniciar();
        bloqueio.aplicar(inicio, fim, diaInteiro, motivo);
        return bloqueio;
    }

    public void atualizar(OffsetDateTime inicio, OffsetDateTime fim,
                          boolean diaInteiro, String motivo) {
        aplicar(inicio, fim, diaInteiro, motivo);
        auditoria.marcarAtualizacao();
    }

    /** Sobreposicao semiaberta: encostar no limite nao conta como conflito. */
    public boolean sobrepoe(OffsetDateTime outroInicio, OffsetDateTime outroFim) {
        return inicio.isBefore(outroFim) && fim.isAfter(outroInicio);
    }

    private void aplicar(OffsetDateTime inicio, OffsetDateTime fim,
                         boolean diaInteiro, String motivo) {
        if (inicio == null || fim == null || !fim.isAfter(inicio)) {
            throw new RegraDeNegocioException("PERIODO_INVALIDO",
                    "O horario de fim deve ser posterior ao horario de inicio.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new RegraDeNegocioException("MOTIVO_OBRIGATORIO",
                    "Informe o motivo do bloqueio.");
        }
        this.inicio = inicio;
        this.fim = fim;
        this.diaInteiro = diaInteiro;
        this.motivo = motivo.trim();
    }
}
