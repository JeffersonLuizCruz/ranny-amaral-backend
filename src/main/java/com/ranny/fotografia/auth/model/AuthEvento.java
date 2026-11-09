package com.ranny.fotografia.auth.model;

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

/** Registro imutavel de evento de autenticacao. */
@Entity
@Table(name = "auth_eventos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthEvento {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "usuario_id", length = 64)
    private String usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuthEventoTipo tipo;

    @Column(length = 64)
    private String ip;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(length = 500)
    private String detalhe;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    public static AuthEvento criar(String usuarioId, AuthEventoTipo tipo, String ip,
                                   String userAgent, String detalhe, Instant agora) {
        AuthEvento evento = new AuthEvento();
        evento.id = UUID.randomUUID().toString();
        evento.usuarioId = usuarioId;
        evento.tipo = tipo;
        evento.ip = ip;
        evento.userAgent = userAgent == null ? null
                : userAgent.substring(0, Math.min(userAgent.length(), 300));
        evento.detalhe = detalhe == null ? null
                : detalhe.substring(0, Math.min(detalhe.length(), 500));
        evento.criadoEm = agora;
        return evento;
    }
}
