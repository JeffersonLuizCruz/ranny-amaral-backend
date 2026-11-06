package com.ranny.fotografia.galeria.model;

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

/** Historico append-only da galeria. */
@Entity
@Table(name = "eventos_galeria")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventoGaleria {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "galeria_id", nullable = false, length = 64)
    private String galeriaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EventoGaleriaTipo tipo;

    @Column(length = 500)
    private String detalhe;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    public static EventoGaleria criar(String galeriaId, EventoGaleriaTipo tipo, String detalhe) {
        EventoGaleria evento = new EventoGaleria();
        evento.id = UUID.randomUUID().toString();
        evento.galeriaId = galeriaId;
        evento.tipo = tipo;
        evento.detalhe = detalhe == null ? null
                : detalhe.substring(0, Math.min(detalhe.length(), 500));
        evento.criadoEm = Instant.now();
        return evento;
    }
}
