package com.ranny.fotografia.galeria.model;

import com.ranny.fotografia.shared.model.Auditoria;
import com.ranny.fotografia.shared.model.FaixaFotoExtraSnapshot;
import com.ranny.fotografia.shared.model.FaixasFotoExtraConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import java.util.List;
import java.util.UUID;

/**
 * Agregado raiz da galeria de um ensaio.
 *
 * <p>PATTERN: Aggregate Root + State Machine.
 *
 * <p>Congela as condicoes do pacote no momento da criacao. As midias apontam
 * para arquivos genericos no object storage; a galeria nao conhece bytes.
 */
@Entity
@Table(name = "galerias")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Galeria {

    @Id
    @Column(length = 64)
    private String id;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(name = "estudio_id", nullable = false, length = 64)
    private String estudioId;

    @Column(name = "agendamento_id", nullable = false, length = 64)
    private String agendamentoId;

    @Column(name = "contrato_id", length = 64)
    private String contratoId;

    @Column(name = "cliente_id", length = 64)
    private String clienteId;

    @Column(name = "cliente_nome", length = 120)
    private String clienteNome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GaleriaStatus status;

    @Column(name = "fotos_inclusas", nullable = false)
    private int fotosInclusas;

    @Column(name = "videos_inclusos", nullable = false)
    private int videosInclusos;

    @Column(name = "valor_foto_extra", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorFotoExtra;

    @Convert(converter = FaixasFotoExtraConverter.class)
    @Column(name = "faixas_foto_extra", columnDefinition = "text")
    private List<FaixaFotoExtraSnapshot> faixasFotoExtra;

    @Column(name = "selecao_atual_id", length = 64)
    private String selecaoAtualId;

    @Column(name = "pedido_extras_id", length = 64)
    private String pedidoExtrasId;

    @Column(name = "raw_liberado", nullable = false)
    private boolean rawLiberado;

    @Column(name = "raw_liberado_em")
    private Instant rawLiberadoEm;

    @Column(name = "raw_liberado_por", length = 120)
    private String rawLiberadoPor;

    @Column(name = "zip_baixado_em")
    private Instant zipBaixadoEm;

    @Embedded
    private Auditoria auditoria;

    public static Galeria criar(String estudioId, String agendamentoId, String contratoId,
                                String clienteId, String clienteNome, int fotosInclusas,
                                int videosInclusos, BigDecimal valorFotoExtra) {
        return criar(estudioId, agendamentoId, contratoId, clienteId, clienteNome,
                fotosInclusas, videosInclusos, valorFotoExtra, List.of());
    }

    public static Galeria criar(String estudioId, String agendamentoId, String contratoId,
                                String clienteId, String clienteNome, int fotosInclusas,
                                int videosInclusos, BigDecimal valorFotoExtra,
                                List<FaixaFotoExtraSnapshot> faixasFotoExtra) {
        Galeria galeria = new Galeria();
        galeria.id = UUID.randomUUID().toString();
        galeria.estudioId = estudioId;
        galeria.agendamentoId = agendamentoId;
        galeria.contratoId = contratoId;
        galeria.clienteId = clienteId;
        galeria.clienteNome = clienteNome;
        galeria.status = GaleriaStatus.AGUARDANDO_UPLOAD;
        galeria.fotosInclusas = Math.max(0, fotosInclusas);
        galeria.videosInclusos = Math.max(0, videosInclusos);
        galeria.valorFotoExtra = valorFotoExtra == null ? BigDecimal.ZERO : valorFotoExtra;
        galeria.faixasFotoExtra = faixasFotoExtra == null
                ? List.of() : List.copyOf(faixasFotoExtra);
        galeria.auditoria = Auditoria.iniciar();
        return galeria;
    }

    /** Copia defensiva das faixas congeladas na criacao da galeria. */
    public List<FaixaFotoExtraSnapshot> getFaixasFotoExtra() {
        return faixasFotoExtra == null ? List.of() : List.copyOf(faixasFotoExtra);
    }

    public void publicarSelecao() {
        if (status == GaleriaStatus.CANCELADA) {
            throw new IllegalStateException("Galeria cancelada nao pode ser publicada.");
        }
        this.status = GaleriaStatus.EM_SELECAO;
        this.auditoria.marcarAtualizacao();
    }

    public void registrarSelecao(String selecaoId, String pedidoExtrasId) {
        this.selecaoAtualId = selecaoId;
        this.pedidoExtrasId = pedidoExtrasId;
        this.status = GaleriaStatus.SELECAO_ENVIADA;
        this.auditoria.marcarAtualizacao();
    }

    public void reabrirSelecao() {
        this.status = GaleriaStatus.EM_SELECAO;
        this.selecaoAtualId = null;
        this.pedidoExtrasId = null;
        this.auditoria.marcarAtualizacao();
    }

    public void iniciarEdicao() {
        this.status = GaleriaStatus.EM_EDICAO;
        this.auditoria.marcarAtualizacao();
    }

    public void publicarEntrega() {
        this.status = GaleriaStatus.ENTREGUE;
        this.auditoria.marcarAtualizacao();
    }

    public void revogarEntrega() {
        this.status = GaleriaStatus.EM_EDICAO;
        this.auditoria.marcarAtualizacao();
    }

    /**
     * Libera os arquivos originais para download.
     *
     * <p>PATTERN: State Machine + Auditoria. A decisao e sempre explicita e
     * reversivel; o motivo/quem fica registrado para rastreabilidade.
     */
    public void liberarRaw(String por) {
        this.rawLiberado = true;
        this.rawLiberadoEm = Instant.now();
        this.rawLiberadoPor = por == null || por.isBlank()
                ? "Equipe do estudio" : por;
        this.auditoria.marcarAtualizacao();
    }

    public void revogarRaw() {
        this.rawLiberado = false;
        this.rawLiberadoEm = null;
        this.rawLiberadoPor = null;
        this.auditoria.marcarAtualizacao();
    }

    /**
     * Registra o primeiro download do ZIP da entrega pelo cliente.
     *
     * <p>Download individual nao marca a galeria; apenas o lote completo conta
     * para o filtro operacional "Baixadas".
     */
    public void registrarZipBaixado() {
        if (this.zipBaixadoEm == null) {
            this.zipBaixadoEm = Instant.now();
            this.auditoria.marcarAtualizacao();
        }
    }

    public void cancelar() {
        this.status = GaleriaStatus.CANCELADA;
        this.auditoria.marcarAtualizacao();
    }

    public boolean aceitaNovasMidias() {
        return status == GaleriaStatus.AGUARDANDO_UPLOAD || status == GaleriaStatus.EM_SELECAO;
    }
}
