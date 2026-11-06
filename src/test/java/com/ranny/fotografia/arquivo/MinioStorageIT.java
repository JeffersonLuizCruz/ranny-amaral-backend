package com.ranny.fotografia.arquivo;

import com.ranny.fotografia.arquivo.storage.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integracao real com MinIO via URLs pre-assinadas.
 *
 * <p>Executado no `verify` apenas quando MINIO_TEST_ENDPOINT estiver definido.
 * Valida o caminho multipart, conclusao, checksum, validacao de conteudo e
 * download direto do storage.
 */
@SpringBootTest
@ActiveProfiles("minio-test")
@EnabledIfEnvironmentVariable(named = "MINIO_TEST_ENDPOINT", matches = ".+")
class MinioStorageIT {

    private static final long MAX_COMPROVANTE = 10L * 1024 * 1024;

    @Autowired
    ArquivoUpload arquivoUpload;

    @Autowired
    S3Client s3;

    @Autowired
    StorageProperties properties;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @BeforeEach
    void prepararBucket() {
        try {
            s3.createBucket(CreateBucketRequest.builder().bucket(properties.getBucket()).build());
        } catch (S3Exception ex) {
            if (ex.statusCode() != 409) {
                throw ex;
            }
        }
    }

    @Test
    void enviaMultipartConcluiEBaixaDoStorage() throws Exception {
        byte[] conteudo = conteudoPdf(9 * 1024 * 1024);
        String referencia = "ct-minio-multipart";
        ArquivoUpload.SessaoUpload sessao = arquivoUpload.iniciar(new ArquivoUpload.IniciarUpload(
                ArquivoFinalidade.COMPROVANTE_PAGAMENTO,
                referencia,
                "testes/minio",
                "comprovante.pdf",
                "application/pdf",
                conteudo.length,
                sha256(conteudo),
                MAX_COMPROVANTE));

        assertThat(sessao.estrategia()).isEqualTo(ArquivoUpload.EstrategiaUpload.MULTIPART);
        assertThat(sessao.partes()).hasSize(2);

        List<ArquivoUpload.ParteEnviada> partes = new ArrayList<>();
        for (ArquivoUpload.UrlParte parte : sessao.partes()) {
            int inicio = (parte.partNumber() - 1) * sessao.partSizeBytes();
            int fim = Math.min(inicio + sessao.partSizeBytes(), conteudo.length);
            HttpResponse<Void> resposta = enviarParte(
                    parte.url(), Arrays.copyOfRange(conteudo, inicio, fim));
            assertThat(resposta.statusCode()).isBetween(200, 299);
            String etag = resposta.headers().firstValue("ETag").orElseThrow();
            partes.add(new ArquivoUpload.ParteEnviada(parte.partNumber(), etag));
        }

        ArquivoUpload.ArquivoResumo resumo = arquivoUpload.concluir(
                sessao.arquivoId(), ArquivoFinalidade.COMPROVANTE_PAGAMENTO, referencia, partes);

        try {
            assertThat(resumo.status()).isEqualTo(ArquivoStatus.DISPONIVEL);
            assertThat(resumo.tamanhoBytes()).isEqualTo(conteudo.length);
            assertThat(resumo.mimeType()).isEqualTo("application/pdf");

            String url = arquivoUpload.urlDownload(
                    sessao.arquivoId(), ArquivoFinalidade.COMPROVANTE_PAGAMENTO, referencia).url();
            HttpResponse<byte[]> download = http.send(
                    HttpRequest.newBuilder(URI.create(url)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            assertThat(download.statusCode()).isEqualTo(200);
            assertThat(download.body()).hasSize(conteudo.length);
        } finally {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(resumo.objectKey())
                    .build());
        }
    }

    @Test
    void enviaPutUnicoEConclui() throws Exception {
        byte[] conteudo = conteudoPdf(1024 * 1024);
        String referencia = "ct-minio-put";
        ArquivoUpload.SessaoUpload sessao = arquivoUpload.iniciar(new ArquivoUpload.IniciarUpload(
                ArquivoFinalidade.COMPROVANTE_PAGAMENTO,
                referencia,
                "testes/minio",
                "comprovante.pdf",
                "application/pdf",
                conteudo.length,
                sha256(conteudo),
                MAX_COMPROVANTE));

        assertThat(sessao.estrategia()).isEqualTo(ArquivoUpload.EstrategiaUpload.PUT);

        HttpResponse<Void> resposta = http.send(
                HttpRequest.newBuilder(URI.create(sessao.partes().get(0).url()))
                        .header("Content-Type", "application/pdf")
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(conteudo))
                        .build(),
                HttpResponse.BodyHandlers.discarding());
        assertThat(resposta.statusCode()).isBetween(200, 299);

        ArquivoUpload.ArquivoResumo resumo = arquivoUpload.concluir(
                sessao.arquivoId(), ArquivoFinalidade.COMPROVANTE_PAGAMENTO, referencia, List.of());

        try {
            assertThat(resumo.status()).isEqualTo(ArquivoStatus.DISPONIVEL);
            assertThat(resumo.tamanhoBytes()).isEqualTo(conteudo.length);
        } finally {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(resumo.objectKey())
                    .build());
        }
    }

    private HttpResponse<Void> enviarParte(String url, byte[] conteudo) throws Exception {
        return http.send(
                HttpRequest.newBuilder(URI.create(url))
                        .header("Content-Type", "application/octet-stream")
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(conteudo))
                        .build(),
                HttpResponse.BodyHandlers.discarding());
    }

    private static byte[] conteudoPdf(int tamanho) {
        byte[] conteudo = new byte[tamanho];
        byte[] assinatura = "%PDF-1.7\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        System.arraycopy(assinatura, 0, conteudo, 0, assinatura.length);
        Arrays.fill(conteudo, assinatura.length, conteudo.length, (byte) 'x');
        return conteudo;
    }

    private static String sha256(byte[] conteudo) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
    }
}
