package com.ranny.fotografia.arquivo.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * PATTERN: Factory / Adapter Configuration.
 *
 * <p>Cria um cliente S3 para operacoes internas (endpoint da rede Docker) e um
 * presigner com o endpoint publico. Em MinIO/S3 compativel o path-style precisa
 * estar habilitado. Sem separar os endpoints, a URL assinada aponta para um host
 * inacessivel ao navegador.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "app.storage.provider", havingValue = "minio", matchIfMissing = true)
    public S3Client s3Interno(StorageProperties properties) {
        return S3Client.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credenciais(properties))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "app.storage.provider", havingValue = "minio", matchIfMissing = true)
    public S3Presigner s3PresignerPublico(StorageProperties properties) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(properties.getPublicEndpoint()))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credenciais(properties))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    private static StaticCredentialsProvider credenciais(StorageProperties properties) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()));
    }
}
