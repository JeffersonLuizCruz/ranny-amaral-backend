package com.ranny.fotografia.galeria.api;

import com.ranny.fotografia.arquivo.ArquivoUpload;
import com.ranny.fotografia.galeria.model.EventoGaleria;
import com.ranny.fotografia.galeria.model.Galeria;
import com.ranny.fotografia.galeria.model.LinkGaleria;
import com.ranny.fotografia.galeria.model.MidiaGaleria;
import com.ranny.fotografia.galeria.model.PedidoExtras;
import com.ranny.fotografia.galeria.model.SelecaoGaleria;
import com.ranny.fotografia.shared.model.FormaPagamentoSaldo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/** DTOs do modulo de galerias. */
public final class GaleriaDtos {

    private GaleriaDtos() {
    }

    public record GaleriaResponse(
            String id,
            String agendamentoId,
            String contratoId,
            String clienteId,
            String clienteNome,
            String status,
            int fotosInclusas,
            int videosInclusos,
            BigDecimal valorFotoExtra,
            long totalFotos,
            long totalVideos,
            long fotosDisponiveis,
            long videosDisponiveis,
            boolean rawLiberado,
            Instant rawLiberadoEm,
            String rawLiberadoPor,
            String capaUrl,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static GaleriaResponse de(Galeria galeria, long totalFotos, long totalVideos,
                                         long fotosDisponiveis, long videosDisponiveis,
                                         String capaUrl) {
            return new GaleriaResponse(
                    galeria.getId(),
                    galeria.getAgendamentoId(),
                    galeria.getContratoId(),
                    galeria.getClienteId(),
                    galeria.getClienteNome(),
                    galeria.getStatus().valor(),
                    galeria.getFotosInclusas(),
                    galeria.getVideosInclusos(),
                    galeria.getValorFotoExtra(),
                    totalFotos,
                    totalVideos,
                    fotosDisponiveis,
                    videosDisponiveis,
                    galeria.isRawLiberado(),
                    galeria.getRawLiberadoEm(),
                    galeria.getRawLiberadoPor(),
                    capaUrl,
                    galeria.getAuditoria().getCriadoEm(),
                    galeria.getAuditoria().getAtualizadoEm());
        }
    }

    public record MidiaResponse(
            String id,
            String tipo,
            String nomeOriginal,
            String mimeType,
            long tamanho,
            Integer largura,
            Integer altura,
            Integer duracaoSegundos,
            int ordem,
            String status,
            String erroProcessamento,
            String thumbUrl,
            String webUrl,
            String originalUrl,
            String editadaUrl,
            boolean editadaDisponivel,
            boolean editadaProcessando,
            Instant createdAt
    ) {
        public static MidiaResponse de(MidiaGaleria midia, String thumbUrl, String webUrl,
                                       String originalUrl, String editadaUrl,
                                       boolean editadaProcessando) {
            return new MidiaResponse(
                    midia.getId(),
                    midia.getTipo().valor(),
                    midia.getNomeOriginal(),
                    midia.getMimeType(),
                    midia.getTamanhoBytes(),
                    midia.getLargura(),
                    midia.getAltura(),
                    midia.getDuracaoSegundos(),
                    midia.getOrdem(),
                    midia.getStatus().name().toLowerCase(Locale.ROOT),
                    midia.getErroProcessamento(),
                    thumbUrl,
                    webUrl,
                    originalUrl,
                    editadaUrl,
                    midia.temVersaoEditada(),
                    editadaProcessando,
                    midia.getAuditoria().getCriadoEm());
        }
    }

    public record LinkResponse(
            String id,
            String tipo,
            String token,
            String status,
            Instant expiraEm,
            Instant visualizadaEm,
            String protocolo
    ) {
        public static LinkResponse de(LinkGaleria link) {
            return new LinkResponse(
                    link.getId(),
                    link.getTipo().name().toLowerCase(Locale.ROOT),
                    link.getToken(),
                    link.getStatus().name().toLowerCase(Locale.ROOT),
                    link.getExpiraEm(),
                    link.getVisualizadaEm(),
                    link.getProtocolo());
        }
    }

    public record SelecaoResponse(
            String id,
            String protocolo,
            String status,
            int fotosSelecionadas,
            int videosSelecionados,
            int fotosExtras,
            BigDecimal valorExtras,
            Instant enviadaEm,
            List<String> fotoIds,
            List<String> videoIds
    ) {
        public static SelecaoResponse de(SelecaoGaleria selecao, List<String> fotoIds,
                                         List<String> videoIds) {
            return new SelecaoResponse(
                    selecao.getId(),
                    selecao.getProtocolo(),
                    selecao.getStatus().name().toLowerCase(Locale.ROOT),
                    selecao.getFotosSelecionadas(),
                    selecao.getVideosSelecionados(),
                    selecao.getFotosExtras(),
                    selecao.getValorExtras(),
                    selecao.getEnviadaEm(),
                    fotoIds,
                    videoIds);
        }
    }

    public record PedidoExtrasResponse(
            String id,
            int quantidadeFotos,
            BigDecimal valorUnitario,
            BigDecimal valorTotal,
            String status,
            String formaPagamento,
            Instant confirmadoEm,
            String confirmadoPor,
            String liberacaoMotivo,
            String liberadoPor,
            Instant liberadoEm
    ) {
        public static PedidoExtrasResponse de(PedidoExtras pedido) {
            return new PedidoExtrasResponse(
                    pedido.getId(),
                    pedido.getQuantidadeFotos(),
                    pedido.getValorUnitario(),
                    pedido.getValorTotal(),
                    pedido.getStatus().name().toLowerCase(Locale.ROOT),
                    pedido.getFormaPagamento() == null ? null
                            : pedido.getFormaPagamento().name().toLowerCase(Locale.ROOT),
                    pedido.getConfirmadoEm(),
                    pedido.getConfirmadoPor(),
                    pedido.getLiberacaoMotivo(),
                    pedido.getLiberadoPor(),
                    pedido.getLiberadoEm());
        }
    }

    public record EventoResponse(String id, String tipo, String detalhe, Instant em) {
        public static EventoResponse de(EventoGaleria evento) {
            return new EventoResponse(
                    evento.getId(),
                    evento.getTipo().name().toLowerCase(Locale.ROOT),
                    evento.getDetalhe(),
                    evento.getCriadoEm());
        }
    }

    public record GaleriaDetalheResponse(
            GaleriaResponse galeria,
            List<LinkResponse> links,
            SelecaoResponse selecao,
            PedidoExtrasResponse extras,
            List<EventoResponse> historico
    ) {
    }

    public record GaleriaPublicaResponse(
            LinkResponse link,
            GaleriaResponse galeria,
            List<MidiaResponse> midias,
            SelecaoResponse selecao,
            PedidoExtrasResponse extras,
            boolean entregaDisponivel
    ) {
    }

    public record CriarGaleriaRequest(@NotBlank String agendamentoId) {
    }

    public record IniciarMidiaUploadRequest(
            @NotBlank String nomeArquivo,
            @NotBlank String mimeType,
            long tamanho,
            @NotBlank String checksumSha256,
            @NotBlank String tipo
    ) {
    }

    public record ConcluirMidiaUploadRequest(
            @NotNull List<ParteRequest> partes,
            @NotBlank String tipo
    ) {
    }

    /** Upload da versao final editada; sempre uma foto. */
    public record IniciarMidiaEditadaUploadRequest(
            @NotBlank String nomeArquivo,
            @NotBlank String mimeType,
            long tamanho,
            @NotBlank String checksumSha256
    ) {
    }

    public record ConcluirMidiaEditadaUploadRequest(@NotNull List<ParteRequest> partes) {
    }

    public record ParteRequest(int partNumber, String etag) {
    }

    public record UploadSessaoResponse(
            String arquivoId,
            String uploadId,
            boolean reutilizado,
            String estrategia,
            long tamanho,
            int partSize,
            List<ParteUrlResponse> partes,
            Instant expiraEm
    ) {
        public static UploadSessaoResponse de(ArquivoUpload.SessaoUpload sessao) {
            return new UploadSessaoResponse(
                    sessao.arquivoId(),
                    sessao.uploadId(),
                    sessao.reutilizado(),
                    sessao.estrategia().name(),
                    sessao.tamanhoBytes(),
                    sessao.partSizeBytes(),
                    sessao.partes().stream()
                            .map(parte -> new ParteUrlResponse(parte.partNumber(), parte.url()))
                            .toList(),
                    sessao.expiraEm());
        }
    }

    public record ParteUrlResponse(int partNumber, String url) {
    }

    public record SelecaoPublicaRequest(List<String> fotoIds, List<String> videoIds) {
    }

    public record ExtrasConfirmacaoRequest(FormaPagamentoSaldo formaPagamento, String confirmadoPor) {
    }

    public record LiberacaoEdicaoRequest(String motivo, String por) {
    }

    /** Liberacao dos originais (RAW); o "por" e auditado no historico. */
    public record LiberacaoRawRequest(String por) {
    }

    public record DownloadRequest(String midiaId, String versao) {
    }

    public record DownloadResponse(String url, Instant expiraEm, String nomeArquivo) {
    }
}
