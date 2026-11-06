package com.ranny.fotografia.arquivo.model;

import com.ranny.fotografia.arquivo.ArquivoFinalidade;
import com.ranny.fotografia.arquivo.ArquivoStatus;
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

/**
 * Metadados de um binario armazenado no object storage.
 *
 * <p>PATTERN: Entity + State Machine.
 *
 * <p>Este agregado nao sabe se o binario e comprovante, foto ou PDF. Ele guarda
 * a finalidade, a localizacao opaca no storage e o ciclo de vida do upload. A
 * entidade {@code Foto}, na fase de galerias, apontara para um arquivo original
 * e para seus derivados.
 *
 * <p>Os bytes nunca passam pelo banco nem pelo processo Java: o navegador
 * envia direto ao MinIO/S3 por URL pre-assinada.
 */
@Entity
@Table(name = "arquivos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Arquivo {

    public static final long VALIDADE_UPLOAD_MINUTOS = 60;

    @Id
    @Column(length = 64)
    private String id;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ArquivoFinalidade finalidade;

    @Column(name = "referencia_id", nullable = false, length = 64)
    private String referenciaId;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(nullable = false, length = 120)
    private String bucket;

    @Column(name = "object_key", nullable = false, length = 512)
    private String objectKey;

    @Column(name = "nome_original", nullable = false, length = 255)
    private String nomeOriginal;

    @Column(name = "mime_type", nullable = false, length = 120)
    private String mimeType;

    @Column(name = "tamanho_bytes", nullable = false)
    private long tamanhoBytes;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ArquivoStatus status;

    @Column(name = "upload_id", length = 255)
    private String uploadId;

    @Embedded
    private Auditoria auditoria;

    @Column(name = "concluido_em")
    private Instant concluidoEm;

    @Column(name = "expira_em")
    private Instant expiraEm;

    public static Arquivo iniciar(String id, ArquivoFinalidade finalidade, String referenciaId,
                                  String provider, String bucket, String objectKey,
                                  String nomeOriginal, String mimeType, long tamanhoBytes,
                                  String checksumSha256, String uploadId) {
        Arquivo arquivo = new Arquivo();
        arquivo.id = id;
        arquivo.finalidade = finalidade;
        arquivo.referenciaId = referenciaId;
        arquivo.provider = provider;
        arquivo.bucket = bucket;
        arquivo.objectKey = objectKey;
        arquivo.nomeOriginal = nomeOriginal;
        arquivo.mimeType = mimeType;
        arquivo.tamanhoBytes = tamanhoBytes;
        arquivo.checksumSha256 = checksumSha256;
        arquivo.status = ArquivoStatus.PENDENTE;
        arquivo.uploadId = uploadId;
        arquivo.auditoria = Auditoria.iniciar();
        arquivo.expiraEm = Instant.now().plusSeconds(VALIDADE_UPLOAD_MINUTOS * 60);
        return arquivo;
    }

    public void marcarEnviando() {
        if (status == ArquivoStatus.PENDENTE) {
            status = ArquivoStatus.ENVIANDO;
            auditoria.marcarAtualizacao();
        }
    }

    public void marcarDisponivel(String mimeDetectado, Instant em) {
        this.status = ArquivoStatus.DISPONIVEL;
        this.mimeType = mimeDetectado;
        this.concluidoEm = em;
        this.uploadId = null;
        this.expiraEm = null;
        this.auditoria.marcarAtualizacao();
    }

    public void falhar() {
        this.status = ArquivoStatus.FALHA;
        this.uploadId = null;
        this.expiraEm = null;
        this.auditoria.marcarAtualizacao();
    }

    public void cancelar() {
        this.status = ArquivoStatus.CANCELADO;
        this.uploadId = null;
        this.expiraEm = null;
        this.auditoria.marcarAtualizacao();
    }

    public boolean estaDisponivel() {
        return status == ArquivoStatus.DISPONIVEL;
    }

    public boolean estaPendente() {
        return status == ArquivoStatus.PENDENTE || status == ArquivoStatus.ENVIANDO;
    }
}
