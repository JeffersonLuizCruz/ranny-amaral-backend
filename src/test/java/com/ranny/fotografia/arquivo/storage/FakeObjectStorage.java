package com.ranny.fotografia.arquivo.storage;

import com.ranny.fotografia.arquivo.ArquivoUpload;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Storage em memoria para os testes rapidos (H2).
 *
 * <p>PATTERN: Test Double (Fake) da porta {@link ObjectStorage}.
 *
 * <p>Os testes de API nao precisam de storage real: este fake simula objetos e
 * permite configurar tamanho, MIME e primeiros bytes para exercitar validacoes.
 */
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "memory")
public class FakeObjectStorage implements ObjectStorage {

    private final Map<String, String> multipartPorUpload = new ConcurrentHashMap<>();
    private final Map<String, byte[]> objetos = new ConcurrentHashMap<>();
    private final Map<String, String> mimes = new ConcurrentHashMap<>();
    private byte[] primeirosSimulados = "%PDF-1.7\nconteudo".getBytes(StandardCharsets.UTF_8);
    private String mimeSimulado = "application/pdf";
    private long tamanhoSimulado = -1;

    public void simular(long tamanho, String mime, byte[] primeirosBytes) {
        this.tamanhoSimulado = tamanho;
        this.mimeSimulado = mime;
        this.primeirosSimulados = primeirosBytes;
    }

    public Map<String, byte[]> objetos() {
        return objetos;
    }

    @Override
    public String criarMultipart(String objectKey, String mimeType) {
        String uploadId = UUID.randomUUID().toString();
        multipartPorUpload.put(uploadId, objectKey);
        return uploadId;
    }

    @Override
    public List<String> urlsPartes(String objectKey, String uploadId, int totalPartes,
                                   Duration validade) {
        List<String> urls = new ArrayList<>(totalPartes);
        for (int parte = 1; parte <= totalPartes; parte++) {
            urls.add("http://fake-storage/%s?uploadId=%s&partNumber=%d"
                    .formatted(objectKey, uploadId, parte));
        }
        return urls;
    }

    @Override
    public String urlPut(String objectKey, String mimeType, Duration validade) {
        return "http://fake-storage/%s?op=put".formatted(objectKey);
    }

    @Override
    public String urlGet(String objectKey, String nomeArquivo, Duration validade) {
        return "http://fake-storage/%s?op=get&filename=%s"
                .formatted(objectKey, nomeArquivo == null ? "arquivo" : nomeArquivo);
    }

    @Override
    public byte[] ler(String objectKey) {
        return objetos.getOrDefault(objectKey, primeirosSimulados);
    }

    @Override
    public InputStream abrir(String objectKey) {
        return new ByteArrayInputStream(ler(objectKey));
    }

    @Override
    public void salvar(String objectKey, byte[] conteudo, String mimeType) {
        objetos.put(objectKey, conteudo);
        mimes.put(objectKey, mimeType);
    }

    @Override
    public ObjectMetadata concluirMultipart(String objectKey, String uploadId,
                                            List<ArquivoUpload.ParteEnviada> partes) {
        multipartPorUpload.remove(uploadId);
        objetos.put(objectKey, primeirosSimulados);
        return metadados(objectKey);
    }

    @Override
    public ObjectMetadata metadados(String objectKey) {
        byte[] conteudo = objetos.get(objectKey);
        long tamanho = tamanhoSimulado >= 0
                ? tamanhoSimulado
                : conteudo == null ? primeirosSimulados.length : conteudo.length;
        return new ObjectMetadata(tamanho, mimes.getOrDefault(objectKey, mimeSimulado), "etag-fake");
    }

    @Override
    public byte[] primeirosBytes(String objectKey, int quantidade) {
        byte[] base = objetos.getOrDefault(objectKey, primeirosSimulados);
        return java.util.Arrays.copyOf(base, Math.min(quantidade, base.length));
    }

    @Override
    public void abortarMultipart(String objectKey, String uploadId) {
        multipartPorUpload.remove(uploadId);
    }

    @Override
    public void remover(String objectKey) {
        objetos.remove(objectKey);
    }
}
