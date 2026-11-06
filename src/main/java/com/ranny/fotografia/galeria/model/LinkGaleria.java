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

import java.time.Instant;
import java.util.UUID;

/** Link publico de selecao ou entrega. */
@Entity
@Table(name = "links_galeria")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LinkGaleria {

    @Id
    @Column(length = 64)
    private String id;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(name = "galeria_id", nullable = false, length = 64)
    private String galeriaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LinkGaleriaTipo tipo;

    @Column(nullable = false, unique = true, length = 80)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LinkGaleriaStatus status;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "visualizada_em")
    private Instant visualizadaEm;

    @Column(length = 30)
    private String protocolo;

    @Embedded
    private Auditoria auditoria;

    public static LinkGaleria criar(String galeriaId, LinkGaleriaTipo tipo, int dias) {
        LinkGaleria link = new LinkGaleria();
        link.id = UUID.randomUUID().toString();
        link.galeriaId = galeriaId;
        link.tipo = tipo;
        link.token = UUID.randomUUID().toString().replace("-", "");
        link.status = LinkGaleriaStatus.ATIVO;
        link.expiraEm = Instant.now().plusSeconds(dias * 86_400L);
        link.auditoria = Auditoria.iniciar();
        return link;
    }

    public void estender(int dias) {
        this.expiraEm = Instant.now().plusSeconds(dias * 86_400L);
        this.status = LinkGaleriaStatus.ATIVO;
        this.auditoria.marcarAtualizacao();
    }

    public void visualizar() {
        if (visualizadaEm == null) {
            this.visualizadaEm = Instant.now();
            this.auditoria.marcarAtualizacao();
        }
    }

    public void expirar() {
        this.status = LinkGaleriaStatus.EXPIRADO;
        this.auditoria.marcarAtualizacao();
    }

    public void revogar() {
        this.status = LinkGaleriaStatus.REVOGADO;
        this.auditoria.marcarAtualizacao();
    }

    public boolean estaAtivo() {
        return status == LinkGaleriaStatus.ATIVO && expiraEm.isAfter(Instant.now());
    }
}
