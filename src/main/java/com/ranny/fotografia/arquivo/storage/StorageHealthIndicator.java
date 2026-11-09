package com.ranny.fotografia.arquivo.storage;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;

/** Verifica se o bucket configurado esta acessivel. */
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3", matchIfMissing = true)
public class StorageHealthIndicator implements HealthIndicator {

    private final S3Client s3;
    private final StorageProperties properties;

    public StorageHealthIndicator(S3Client s3, StorageProperties properties) {
        this.s3 = s3;
        this.properties = properties;
    }

    @Override
    public Health health() {
        try {
            s3.headBucket(bucket -> bucket.bucket(properties.getBucket()));
            return Health.up()
                    .withDetail("bucket", properties.getBucket())
                    .withDetail("endpoint", properties.getEndpoint())
                    .build();
        } catch (RuntimeException ex) {
            return Health.down(ex)
                    .withDetail("bucket", properties.getBucket())
                    .withDetail("endpoint", properties.getEndpoint())
                    .build();
        }
    }
}
