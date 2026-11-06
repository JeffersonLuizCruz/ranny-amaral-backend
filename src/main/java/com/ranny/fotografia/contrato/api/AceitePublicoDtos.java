package com.ranny.fotografia.contrato.api;

import com.ranny.fotografia.shared.model.ComprovantePagamento;
import com.ranny.fotografia.shared.model.TipoDocumento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * DTOs das rotas publicas de aceite.
 *
 * <p>PATTERN: DTO / Form Object + Factory Method ({@code paraModelo}).
 *
 * <p>O comprovante enviado pelo cliente vira um value object do dominio; o
 * arquivo em si sera tratado pelo modulo de storage na proxima fase. O contrato
 * guarda apenas a referencia e os metadados.
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
            String whatsapp,

            @Valid
            ComprovanteRequest comprovante
    ) {
    }

    public record ComprovanteRequest(
            @NotBlank(message = "nao pode ser vazio")
            String arquivoKey,

            @NotBlank(message = "nao pode ser vazio")
            String nomeArquivo,

            @NotBlank(message = "nao pode ser vazio")
            String mimeType,

            @PositiveOrZero(message = "deve ser maior ou igual a zero")
            long tamanho
    ) {

        public ComprovantePagamento paraModelo() {
            return new ComprovantePagamento(arquivoKey, nomeArquivo, mimeType, tamanho, Instant.now());
        }
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
