package com.ranny.fotografia.galeria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Registro de download de uma midia entregue. */
@Entity
@Table(name = "downloads_galeria")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DownloadGaleria {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "galeria_id", nullable = false, length = 64)
    private String galeriaId;

    @Column(name = "link_id", nullable = false, length = 64)
    private String linkId;

    @Column(name = "midia_id", length = 64)
    private String midiaId;

    @Column(name = "arquivo_id", nullable = false, length = 64)
    private String arquivoId;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(name = "baixado_em", nullable = false)
    private Instant baixadoEm;

    public static DownloadGaleria criar(String galeriaId, String linkId, String midiaId,
                                        String arquivoId, String ip, String userAgent) {
        DownloadGaleria download = new DownloadGaleria();
        download.id = UUID.randomUUID().toString();
        download.galeriaId = galeriaId;
        download.linkId = linkId;
        download.midiaId = midiaId;
        download.arquivoId = arquivoId;
        download.ip = ip;
        download.userAgent = userAgent;
        download.baixadoEm = Instant.now();
        return download;
    }
}
