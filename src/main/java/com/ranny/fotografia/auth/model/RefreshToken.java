package com.ranny.fotografia.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Refresh token opaco, persistido apenas como hash SHA-256. */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "usuario_id", nullable = false, length = 64)
    private String usuarioId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "revogado_em")
    private Instant revogadoEm;

    @Column(name = "substituido_por", length = 64)
    private String substituidoPor;

    @Column(length = 64)
    private String ip;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    public static RefreshToken criar(String usuarioId, String tokenHash, Instant expiraEm,
                                     String ip, String userAgent, Instant agora) {
        RefreshToken token = new RefreshToken();
        token.id = UUID.randomUUID().toString();
        token.usuarioId = usuarioId;
        token.tokenHash = tokenHash;
        token.expiraEm = expiraEm;
        token.ip = ip;
        token.userAgent = userAgent;
        token.criadoEm = agora;
        return token;
    }

    public boolean estaAtivo(Instant agora) {
        return revogadoEm == null && expiraEm.isAfter(agora);
    }

    public void revogar(Instant agora) {
        if (revogadoEm == null) {
            revogadoEm = agora;
        }
    }

    public void marcarSubstituido(String novoTokenId, Instant agora) {
        this.substituidoPor = novoTokenId;
        revogar(agora);
    }
}
