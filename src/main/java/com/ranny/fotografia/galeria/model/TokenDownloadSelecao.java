package com.ranny.fotografia.galeria.model;

import com.ranny.fotografia.shared.model.Auditoria;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Capability token de curta duracao para download interno da selecao.
 *
 * <p>Emitido por uma rota autenticada e consumido por uma rota publica de
 * streaming; assim o JWT nunca precisa ser colocado na URL do navegador.
 */
@Entity
@Table(name = "tokens_download_selecao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TokenDownloadSelecao {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "galeria_id", nullable = false, length = 64)
    private String galeriaId;

    @Column(name = "selecao_id", nullable = false, length = 64)
    private String selecaoId;

    @Column(nullable = false, unique = true, length = 80)
    private String token;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Embedded
    private Auditoria auditoria;

    public static TokenDownloadSelecao criar(String galeriaId, String selecaoId,
                                             Duration validade) {
        TokenDownloadSelecao download = new TokenDownloadSelecao();
        download.id = UUID.randomUUID().toString();
        download.galeriaId = galeriaId;
        download.selecaoId = selecaoId;
        download.token = UUID.randomUUID().toString().replace("-", "");
        download.expiraEm = Instant.now().plus(validade);
        download.auditoria = Auditoria.iniciar();
        return download;
    }

    public boolean estaValido() {
        return expiraEm.isAfter(Instant.now());
    }
}
