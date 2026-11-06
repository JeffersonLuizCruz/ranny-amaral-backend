package com.ranny.fotografia.contrato.api;

import com.ranny.fotografia.arquivo.ArquivoUpload;
import com.ranny.fotografia.shared.model.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * DTOs das rotas publicas de aceite e upload de comprovante.
 *
 * <p>PATTERN: DTO / Form Object.
 *
 * <p>O arquivo e enviado em duas fases (iniciar e concluir) direto ao object
 * storage. O aceite do contrato continua sendo um JSON independente.
 */
public final class AceitePublicoDtos {

    private AceitePublicoDtos() {
    }

    public record AceiteRequest(
            @NotBlank(message = "nao pode ser vazio")
            @Size(max = 160, message = "deve ter no maximo 160 caracteres")
            String nomeCompleto,

            @NotNull(message = "nao pode ser nulo")
            TipoDocumento tipoDocumento,

            @NotBlank(message = "nao pode ser vazio")
            @Size(max = 30, message = "deve ter no maximo 30 caracteres")
            String documento,

            @NotBlank(message = "nao pode ser vazio")
            @Size(max = 120, message = "deve ter no maximo 120 caracteres")
            String cidade,

            @Size(max = 30, message = "deve ter no maximo 30 caracteres")
            String whatsapp
    ) {
    }

    public record IniciarUploadRequest(
            @NotBlank(message = "nao pode ser vazio")
            @Size(max = 255, message = "deve ter no maximo 255 caracteres")
            String nomeArquivo,

            @NotBlank(message = "nao pode ser vazio")
            @Size(max = 120, message = "deve ter no maximo 120 caracteres")
            String mimeType,

            long tamanho,

            @NotBlank(message = "nao pode ser vazio")
            String checksumSha256
    ) {
    }

    public record ConcluirUploadRequest(
            @NotNull(message = "nao pode ser nulo")
            List<ParteEnviadaRequest> partes
    ) {
    }

    public record ParteEnviadaRequest(
            @Positive(message = "deve ser maior que zero")
            int partNumber,

            @NotBlank(message = "nao pode ser vazio")
            String etag
    ) {
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

    public record UploadStatusResponse(String arquivoId, String status, boolean disponivel) {
    }

    public record AceiteResponse(
            String protocolo,
            ContratoResponse contrato,
            SolicitacaoAceiteResponse solicitacao
    ) {
    }

    public record ComprovanteResponse(
            ContratoResponse contrato,
            SolicitacaoAceiteResponse solicitacao
    ) {
    }

    public record ContratoPublicoResponse(
            SolicitacaoAceiteResponse solicitacao,
            ContratoResponse contrato
    ) {
    }
}
