package com.ranny.fotografia.galeria.model;

import com.ranny.fotografia.shared.model.Auditoria;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Snapshot da selecao enviada pelo cliente. */
@Entity
@Table(name = "selecoes_galeria")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SelecaoGaleria {

    @Id
    @Column(length = 64)
    private String id;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(name = "galeria_id", nullable = false, length = 64)
    private String galeriaId;

    @Column(nullable = false, unique = true, length = 30)
    private String protocolo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SelecaoStatus status;

    @Column(name = "fotos_selecionadas", nullable = false)
    private int fotosSelecionadas;

    @Column(name = "videos_selecionados", nullable = false)
    private int videosSelecionados;

    @Column(name = "fotos_extras", nullable = false)
    private int fotosExtras;

    @Column(name = "valor_extras", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorExtras;

    @Column(name = "enviada_em", nullable = false)
    private Instant enviadaEm;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Embedded
    private Auditoria auditoria;

    public static SelecaoGaleria criar(String galeriaId, String protocolo,
                                       int fotosSelecionadas, int videosSelecionados,
                                       int fotosExtras, BigDecimal valorExtras,
                                       String ip, String userAgent) {
        SelecaoGaleria selecao = new SelecaoGaleria();
        selecao.id = UUID.randomUUID().toString();
        selecao.galeriaId = galeriaId;
        selecao.protocolo = protocolo;
        selecao.status = SelecaoStatus.ENVIADA;
        selecao.fotosSelecionadas = fotosSelecionadas;
        selecao.videosSelecionados = videosSelecionados;
        selecao.fotosExtras = fotosExtras;
        selecao.valorExtras = valorExtras;
        selecao.enviadaEm = Instant.now();
        selecao.ip = ip;
        selecao.userAgent = userAgent;
        selecao.auditoria = Auditoria.iniciar();
        return selecao;
    }

    public void reabrir() {
        this.status = SelecaoStatus.REABERTA;
        this.auditoria.marcarAtualizacao();
    }
}
