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

/**
 * Ativo semantico da galeria: uma foto ou video.
 *
 * <p>PATTERN: Entity + State Machine.
 *
 * <p>O binario fica em {@code arquivos}; aqui ficam tipo, ordem, dimensoes e o
 * ciclo de processamento. As fotos possuem derivados (thumb/web); videos usam
 * o original nesta fase.
 */
@Entity
@Table(name = "midias_galeria")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MidiaGaleria {

    @Id
    @Column(length = 64)
    private String id;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(name = "galeria_id", nullable = false, length = 64)
    private String galeriaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MidiaTipo tipo;

    @Column(name = "arquivo_original_id", nullable = false, length = 64)
    private String arquivoOriginalId;

    @Column(name = "arquivo_editado_id", length = 64)
    private String arquivoEditadoId;

    @Column(name = "nome_original", nullable = false, length = 255)
    private String nomeOriginal;

    @Column(name = "mime_type", nullable = false, length = 120)
    private String mimeType;

    @Column(name = "tamanho_bytes", nullable = false)
    private long tamanhoBytes;

    private Integer largura;

    private Integer altura;

    @Column(name = "duracao_segundos")
    private Integer duracaoSegundos;

    @Column(nullable = false)
    private int ordem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MidiaStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MidiaFinalidade finalidade = MidiaFinalidade.EDICAO;

    @Column(name = "erro_processamento", length = 500)
    private String erroProcessamento;

    @Column(name = "liberada_em")
    private Instant liberadaEm;

    @Column(name = "entregue_em")
    private Instant entregueEm;

    @Column(name = "liberacao_automatica", nullable = false)
    private boolean liberacaoAutomatica = true;

    @Embedded
    private Auditoria auditoria;

    public static MidiaGaleria criar(String galeriaId, MidiaTipo tipo, String arquivoOriginalId,
                                     String nomeOriginal, String mimeType, long tamanhoBytes,
                                     int ordem) {
        return criar(galeriaId, tipo, arquivoOriginalId, nomeOriginal, mimeType,
                tamanhoBytes, ordem, MidiaFinalidade.EDICAO);
    }

    public static MidiaGaleria criar(String galeriaId, MidiaTipo tipo, String arquivoOriginalId,
                                     String nomeOriginal, String mimeType, long tamanhoBytes,
                                     int ordem, MidiaFinalidade finalidade) {
        MidiaGaleria midia = new MidiaGaleria();
        midia.id = UUID.randomUUID().toString();
        midia.galeriaId = galeriaId;
        midia.tipo = tipo;
        midia.arquivoOriginalId = arquivoOriginalId;
        midia.nomeOriginal = nomeOriginal;
        midia.mimeType = mimeType;
        midia.tamanhoBytes = tamanhoBytes;
        midia.ordem = ordem;
        midia.status = tipo == MidiaTipo.FOTO ? MidiaStatus.PROCESSANDO : MidiaStatus.DISPONIVEL;
        midia.finalidade = finalidade == null ? MidiaFinalidade.EDICAO : finalidade;
        midia.liberacaoAutomatica = true;
        midia.auditoria = Auditoria.iniciar();
        return midia;
    }

    public void disponivel(Integer largura, Integer altura, Integer duracaoSegundos) {
        this.status = MidiaStatus.DISPONIVEL;
        this.largura = largura;
        this.altura = altura;
        this.duracaoSegundos = duracaoSegundos;
        this.erroProcessamento = null;
        this.auditoria.marcarAtualizacao();
    }

    public void falhar(String erro) {
        this.status = MidiaStatus.FALHA;
        this.erroProcessamento = erro == null ? null
                : erro.substring(0, Math.min(erro.length(), 500));
        this.auditoria.marcarAtualizacao();
    }

    public void remover() {
        this.status = MidiaStatus.REMOVIDA;
        this.auditoria.marcarAtualizacao();
    }

    public void vincularEditada(String arquivoEditadoId, Integer largura, Integer altura) {
        this.arquivoEditadoId = arquivoEditadoId;
        this.largura = largura == null ? this.largura : largura;
        this.altura = altura == null ? this.altura : altura;
        this.auditoria.marcarAtualizacao();
    }

    public void removerEditada() {
        this.arquivoEditadoId = null;
        this.auditoria.marcarAtualizacao();
    }

    public boolean temVersaoEditada() {
        return arquivoEditadoId != null;
    }

    public boolean estaDisponivel() {
        return status == MidiaStatus.DISPONIVEL;
    }

    /** A foto ja foi publicada pronta e nao precisa passar pela edicao. */
    public boolean prontaNaOrigem() {
        return finalidade == MidiaFinalidade.PRONTA;
    }

    /**
     * Regra unica de "esta midia tem versao final".
     *
     * <p>Fotos PRONTA usam o proprio arquivo original; fotos EDICAO exigem a
     * versao editada ou a liberacao explicita dos RAW.
     */
    public boolean prontaParaEntrega(boolean rawLiberado) {
        if (prontaNaOrigem()) {
            return estaDisponivel();
        }
        if (tipo == MidiaTipo.VIDEO) {
            return estaDisponivel();
        }
        if (temVersaoEditada()) {
            return true;
        }
        return rawLiberado && estaDisponivel();
    }

    public boolean estaLiberada() {
        return liberadaEm != null;
    }

    public boolean estaEntregue() {
        return entregueEm != null;
    }

    public void liberar() {
        Instant agora = Instant.now();
        if (liberadaEm == null) {
            liberadaEm = agora;
        }
        if (entregueEm == null) {
            entregueEm = agora;
        }
        auditoria.marcarAtualizacao();
    }

    public void entregar() {
        if (entregueEm == null) {
            entregueEm = Instant.now();
        }
        auditoria.marcarAtualizacao();
    }

    public void revogarLiberacao() {
        liberadaEm = null;
        auditoria.marcarAtualizacao();
    }

    public void definirLiberacaoAutomatica(boolean liberacaoAutomatica) {
        this.liberacaoAutomatica = liberacaoAutomatica;
        auditoria.marcarAtualizacao();
    }

    public void definirFinalidade(MidiaFinalidade finalidade) {
        this.finalidade = finalidade == null ? MidiaFinalidade.EDICAO : finalidade;
        auditoria.marcarAtualizacao();
    }
}
