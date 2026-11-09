package com.ranny.fotografia.arquivo.storage;

import com.ranny.fotografia.arquivo.ArquivoUpload;

import java.time.Duration;
import java.util.List;

/**
 * Porta de object storage.
 *
 * <p>PATTERN: Ports & Adapters / Strategy.
 *
 * <p>O restante do backend nunca conhece Garage, S3 ou disco. Trocar o provedor
 * exige apenas outra implementacao desta interface. Os bytes sao enviados e
 * lidos diretamente pelo cliente por URLs pre-assinadas.
 */
public interface ObjectStorage {

    String criarMultipart(String objectKey, String mimeType);

    List<String> urlsPartes(String objectKey, String uploadId, int totalPartes, Duration validade);

    String urlPut(String objectKey, String mimeType, Duration validade);

    String urlGet(String objectKey, String nomeArquivo, Duration validade);

    byte[] ler(String objectKey);

    /** Abre um stream de leitura para downloads em lote (ex.: ZIP). */
    java.io.InputStream abrir(String objectKey);

    void salvar(String objectKey, byte[] conteudo, String mimeType);

    ObjectMetadata concluirMultipart(String objectKey, String uploadId,
                                     List<ArquivoUpload.ParteEnviada> partes);

    ObjectMetadata metadados(String objectKey);

    byte[] primeirosBytes(String objectKey, int quantidade);

    void abortarMultipart(String objectKey, String uploadId);

    void remover(String objectKey);

    record ObjectMetadata(long tamanhoBytes, String mimeType, String etag) {
    }
}
