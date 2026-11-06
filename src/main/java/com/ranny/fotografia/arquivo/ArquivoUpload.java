package com.ranny.fotografia.arquivo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * API publica do modulo {@code arquivo}.
 *
 * <p>PATTERN: Port / Public API do Spring Modulith.
 *
 * <p>Outros modulos nao conhecem a entidade {@code Arquivo} nem o storage. Eles
 * solicitam uma sessao de upload, concluem o envio e recebem um resumo imutavel.
 * O contrato e a galeria usam exatamente o mesmo fluxo.
 */
public interface ArquivoUpload {

    SessaoUpload iniciar(IniciarUpload comando);

    ArquivoResumo concluir(String arquivoId, ArquivoFinalidade finalidade,
                           String referenciaId, List<ParteEnviada> partes);

    void abortar(String arquivoId, ArquivoFinalidade finalidade, String referenciaId);

    Optional<ArquivoResumo> buscar(String arquivoId, ArquivoFinalidade finalidade,
                                   String referenciaId);

    /** Limite configurado para a finalidade (comprovante: 10 MB nesta fase). */
    long tamanhoMaximo(ArquivoFinalidade finalidade);

    /** Gera URL temporaria de leitura; o backend nao transporta os bytes. */
    UrlDownload urlDownload(String arquivoId, ArquivoFinalidade finalidade, String referenciaId);

    /** Le o conteudo para processamento interno (ex.: gerar derivados). */
    byte[] abrirConteudo(String arquivoId, ArquivoFinalidade finalidade, String referenciaId);

    /** Salva um arquivo gerado pelo proprio backend (ex.: miniatura web). */
    ArquivoResumo salvarDisponivel(IniciarArquivo comando, byte[] conteudo);

    record IniciarArquivo(
            ArquivoFinalidade finalidade,
            String referenciaId,
            String prefixo,
            String nomeOriginal,
            String mimeType
    ) {
    }

    record UrlDownload(String url, Instant expiraEm) {
    }

    enum EstrategiaUpload {
        PUT,
        MULTIPART,
        REUTILIZADO
    }

    record IniciarUpload(
            ArquivoFinalidade finalidade,
            String referenciaId,
            String prefixo,
            String nomeOriginal,
            String mimeType,
            long tamanhoBytes,
            String checksumSha256,
            long tamanhoMaximoBytes
    ) {
    }

    record ParteEnviada(int partNumber, String etag) {
    }

    record UrlParte(int partNumber, String url) {
    }

    record SessaoUpload(
            String arquivoId,
            String uploadId,
            boolean reutilizado,
            EstrategiaUpload estrategia,
            long tamanhoBytes,
            int partSizeBytes,
            List<UrlParte> partes,
            Instant expiraEm
    ) {
    }

    record ArquivoResumo(
            String id,
            ArquivoFinalidade finalidade,
            String referenciaId,
            String objectKey,
            String nomeOriginal,
            String mimeType,
            long tamanhoBytes,
            String checksumSha256,
            ArquivoStatus status,
            Instant concluidoEm
    ) {
    }
}
