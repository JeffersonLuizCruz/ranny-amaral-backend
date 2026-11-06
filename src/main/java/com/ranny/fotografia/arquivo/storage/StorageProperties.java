package com.ranny.fotografia.arquivo.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao do storage S3-compativel.
 *
 * <p>PATTERN: Configuration Properties.
 *
 * <p>O endpoint interno e usado pelo backend; o endpoint publico e usado para
 * assinar URLs que o navegador acessa. Em VPS/MinIO eles normalmente sao
 * diferentes (rede Docker x dominio HTTPS).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String provider = "minio";
    private String endpoint = "http://localhost:9000";
    private String publicEndpoint = "http://localhost:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String bucket = "crm-originais";
    private String region = "us-east-1";
    private long multipartThresholdBytes = 5L * 1024 * 1024;
    private int partSizeBytes = 8 * 1024 * 1024;
    private int presignSeconds = 600;
    private long maxComprovanteBytes = 10L * 1024 * 1024;
}
