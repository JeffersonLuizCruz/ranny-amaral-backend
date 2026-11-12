package com.ranny.fotografia.armazenamento.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Amostra periodica do consumo de disco.
 *
 * <p>Histórico append-only: cada coleta registra o total/livre do filesystem e o
 * consumo do CRM separado do consumo de infraestrutura.
 */
@Entity
@Table(name = "armazenamento_amostra")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AmostraArmazenamento {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "coletado_em", nullable = false)
    private Instant coletadoEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_coleta", nullable = false, length = 30)
    private OrigemColeta origemColeta;

    @Column(name = "total_bytes")
    private Long totalBytes;

    @Column(name = "livre_bytes")
    private Long livreBytes;

    @Column(name = "usado_bytes")
    private Long usadoBytes;

    @Column(name = "objetos_bytes", nullable = false)
    private long objetosBytes;

    @Column(name = "objetos_total", nullable = false)
    private long objetosTotal;

    @Column(name = "postgres_bytes")
    private Long postgresBytes;

    @Column(name = "outros_bytes")
    private Long outrosBytes;

    @Column(name = "crm_bytes")
    private Long crmBytes;

    @Column(name = "originais_bytes", nullable = false)
    private long originaisBytes;

    @Column(name = "editadas_bytes", nullable = false)
    private long editadasBytes;

    @Column(name = "derivados_bytes", nullable = false)
    private long derivadosBytes;

    @Column(name = "videos_bytes", nullable = false)
    private long videosBytes;

    @Column(name = "comprovantes_bytes", nullable = false)
    private long comprovantesBytes;

    @Column(name = "reserva_bytes", nullable = false)
    private long reservaBytes;

    public static AmostraArmazenamento registrar(
            OrigemColeta origem,
            Long totalBytes,
            Long livreBytes,
            long objetosBytes,
            long objetosTotal,
            Long postgresBytes,
            long originaisBytes,
            long editadasBytes,
            long derivadosBytes,
            long videosBytes,
            long comprovantesBytes,
            long reservaBytes) {
        AmostraArmazenamento amostra = new AmostraArmazenamento();
        amostra.id = UUID.randomUUID().toString();
        amostra.coletadoEm = Instant.now();
        amostra.origemColeta = origem;
        amostra.totalBytes = totalBytes;
        amostra.livreBytes = livreBytes;
        amostra.usadoBytes = totalBytes == null || livreBytes == null
                ? null : Math.max(0, totalBytes - livreBytes);
        amostra.objetosBytes = objetosBytes;
        amostra.objetosTotal = objetosTotal;
        amostra.postgresBytes = postgresBytes;
        amostra.originaisBytes = originaisBytes;
        amostra.editadasBytes = editadasBytes;
        amostra.derivadosBytes = derivadosBytes;
        amostra.videosBytes = videosBytes;
        amostra.comprovantesBytes = comprovantesBytes;
        amostra.reservaBytes = reservaBytes;
        amostra.crmBytes = objetosBytes + (postgresBytes == null ? 0 : postgresBytes);
        amostra.outrosBytes = amostra.usadoBytes == null
                ? null : Math.max(0, amostra.usadoBytes - amostra.crmBytes);
        return amostra;
    }
}
