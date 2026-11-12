package com.ranny.fotografia.arquivo.storage;

import com.ranny.fotografia.arquivo.ArquivoUpload;
import com.ranny.fotografia.shared.exception.ArmazenamentoException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter S3 compativel com Garage.
 *
 * <p>PATTERN: Adapter.
 *
 * <p>Operacoes de escrita/leitura de metadados usam o endpoint interno. As URLs
 * entregues ao navegador sao assinadas pelo presigner configurado com o endpoint
 * publico.
 */
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3", matchIfMissing = true)
public class S3ObjectStorage implements ObjectStorage {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final StorageProperties properties;

    public S3ObjectStorage(S3Client s3, S3Presigner presigner, StorageProperties properties) {
        this.s3 = s3;
        this.presigner = presigner;
        this.properties = properties;
    }

    @Override
    public String criarMultipart(String objectKey, String mimeType) {
        try {
            return s3.createMultipartUpload(CreateMultipartUploadRequest.builder()
                            .bucket(properties.getBucket())
                            .key(objectKey)
                            .contentType(mimeType)
                            .build())
                    .uploadId();
        } catch (SdkException ex) {
            throw new ArmazenamentoException("Falha ao iniciar multipart upload.", ex);
        }
    }

    @Override
    public List<String> urlsPartes(String objectKey, String uploadId, int totalPartes,
                                   Duration validade) {
        List<String> urls = new ArrayList<>(totalPartes);
        try {
            for (int parte = 1; parte <= totalPartes; parte++) {
                PresignedUploadPartRequest request = presigner.presignUploadPart(
                        UploadPartPresignRequest.builder()
                                .signatureDuration(validade)
                                .uploadPartRequest(UploadPartRequest.builder()
                                        .bucket(properties.getBucket())
                                        .key(objectKey)
                                        .uploadId(uploadId)
                                        .partNumber(parte)
                                        .build())
                                .build());
                urls.add(request.url().toString());
            }
            return urls;
        } catch (SdkException ex) {
            throw new ArmazenamentoException("Falha ao assinar partes do upload.", ex);
        }
    }

    @Override
    public String urlPut(String objectKey, String mimeType, Duration validade) {
        try {
            PresignedPutObjectRequest request = presigner.presignPutObject(
                    PutObjectPresignRequest.builder()
                            .signatureDuration(validade)
                            .putObjectRequest(PutObjectRequest.builder()
                                    .bucket(properties.getBucket())
                                    .key(objectKey)
                                    .contentType(mimeType)
                                    .build())
                            .build());
            return request.url().toString();
        } catch (SdkException ex) {
            throw new ArmazenamentoException("Falha ao assinar upload unico.", ex);
        }
    }

    @Override
    public String urlGet(String objectKey, String nomeArquivo, Duration validade) {
        try {
            PresignedGetObjectRequest request = presigner.presignGetObject(
                    GetObjectPresignRequest.builder()
                            .signatureDuration(validade)
                            .getObjectRequest(GetObjectRequest.builder()
                                    .bucket(properties.getBucket())
                                    .key(objectKey)
                                    .responseContentDisposition(contentDisposition(nomeArquivo))
                                    .build())
                            .build());
            return request.url().toString();
        } catch (SdkException ex) {
            throw new ArmazenamentoException("Falha ao assinar download.", ex);
        }
    }

    private static String contentDisposition(String nomeArquivo) {
        String nome = nomeArquivo == null || nomeArquivo.isBlank()
                ? "arquivo" : nomeArquivo;
        String limpo = nome.replace("\\", "_")
                .replace("\"", "_")
                .replace("\r", "_")
                .replace("\n", "_");
        return "attachment; filename=\"" + limpo + "\"";
    }

    @Override
    public byte[] ler(String objectKey) {
        try {
            ResponseBytes<?> bytes = s3.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .build());
            return bytes.asByteArray();
        } catch (SdkException ex) {
            throw new ArmazenamentoException("Falha ao ler objeto do storage.", ex);
        }
    }

    @Override
    public InputStream abrir(String objectKey) {
        try {
            ResponseInputStream<GetObjectResponse> stream = s3.getObject(
                    GetObjectRequest.builder()
                            .bucket(properties.getBucket())
                            .key(objectKey)
                            .build());
            return stream;
        } catch (SdkException ex) {
            throw new ArmazenamentoException("Falha ao abrir objeto do storage.", ex);
        }
    }

    @Override
    public void salvar(String objectKey, byte[] conteudo, String mimeType) {
        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(properties.getBucket())
                            .key(objectKey)
                            .contentType(mimeType)
                            .build(),
                    RequestBody.fromBytes(conteudo));
        } catch (SdkException ex) {
            throw new ArmazenamentoException("Falha ao salvar objeto no storage.", ex);
        }
    }

    @Override
    public ObjectMetadata concluirMultipart(String objectKey, String uploadId,
                                            List<ArquivoUpload.ParteEnviada> partes) {
        try {
            List<CompletedPart> completedParts = partes.stream()
                    .map(parte -> CompletedPart.builder()
                            .partNumber(parte.partNumber())
                            .eTag(semAspas(parte.etag()))
                            .build())
                    .toList();

            s3.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder()
                            .parts(completedParts)
                            .build())
                    .build());

            return metadados(objectKey);
        } catch (SdkException ex) {
            throw new ArmazenamentoException("Falha ao concluir multipart upload.", ex);
        }
    }

    @Override
    public ObjectMetadata metadados(String objectKey) {
        try {
            HeadObjectResponse head = s3.headObject(HeadObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .build());
            return new ObjectMetadata(head.contentLength(), head.contentType(), head.eTag());
        } catch (SdkException ex) {
            throw new ArmazenamentoException("Falha ao consultar metadados do objeto.", ex);
        }
    }

    @Override
    public byte[] primeirosBytes(String objectKey, int quantidade) {
        try {
            ResponseBytes<?> bytes = s3.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .range("bytes=0-" + (quantidade - 1))
                    .build());
            return bytes.asByteArray();
        } catch (SdkException ex) {
            throw new ArmazenamentoException("Falha ao validar o conteudo do objeto.", ex);
        }
    }

    @Override
    public void abortarMultipart(String objectKey, String uploadId) {
        try {
            s3.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .uploadId(uploadId)
                    .build());
        } catch (SdkException ex) {
            throw new ArmazenamentoException("Falha ao abortar multipart upload.", ex);
        }
    }

    @Override
    public void remover(String objectKey) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .build());
        } catch (SdkException ex) {
            throw new ArmazenamentoException("Falha ao remover objeto do storage.", ex);
        }
    }

    @Override
    public List<ObjetoArmazenado> listar(String prefixo) {
        List<ObjetoArmazenado> objetos = new ArrayList<>();
        try {
            ListObjectsV2Iterable paginas = s3.listObjectsV2Paginator(
                    ListObjectsV2Request.builder()
                            .bucket(properties.getBucket())
                            .prefix(prefixo)
                            .build());
            for (ListObjectsV2Response pagina : paginas) {
                for (S3Object objeto : pagina.contents()) {
                    objetos.add(new ObjetoArmazenado(objeto.key(), objeto.size()));
                }
            }
            return objetos;
        } catch (SdkException ex) {
            throw new ArmazenamentoException("Falha ao listar objetos do storage.", ex);
        }
    }

    @Override
    public int removerLote(List<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return 0;
        }
        int removidos = 0;
        try {
            for (int inicio = 0; inicio < objectKeys.size(); inicio += 1000) {
                List<ObjectIdentifier> lote = objectKeys
                        .subList(inicio, Math.min(inicio + 1000, objectKeys.size()))
                        .stream()
                        .map(chave -> ObjectIdentifier.builder().key(chave).build())
                        .toList();
                DeleteObjectsResponse resposta = s3.deleteObjects(DeleteObjectsRequest.builder()
                        .bucket(properties.getBucket())
                        .delete(Delete.builder().objects(lote).build())
                        .build());
                if (resposta.hasErrors() && !resposta.errors().isEmpty()) {
                    throw new ArmazenamentoException("Falha ao remover parte dos objetos: "
                            + resposta.errors().getFirst().message());
                }
                removidos += lote.size();
            }
            return removidos;
        } catch (SdkException ex) {
            throw new ArmazenamentoException("Falha ao remover objetos em lote.", ex);
        }
    }

    private static String semAspas(String etag) {
        if (etag == null) {
            return null;
        }
        return etag.replace("\"", "");
    }
}
