package dev.monkeypatch.rctiming.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.net.URI;

@Configuration
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    @Value("${storage.endpoint}")
    private String endpoint;

    @Value("${storage.accessKey}")
    private String accessKey;

    @Value("${storage.secretKey}")
    private String secretKey;

    @Value("${storage.region}")
    private String region;

    @Value("${storage.bucket}")
    private String bucket;

    @Bean
    public S3Client s3Client() {
        // forcePathStyle(true) is REQUIRED for MinIO (no virtual-host-style routing).
        // See RESEARCH.md — AWS SDK v2 default is virtual-host-style which breaks MinIO.
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    /**
     * Auto-create the configured bucket at startup if it does not exist.
     *
     * Using ApplicationRunner instead of @PostConstruct avoids a circular reference:
     * a @PostConstruct on a @Configuration class that calls its own @Bean method
     * triggers "bean currently in creation" because Spring hasn't finished registering
     * the bean yet. ApplicationRunner receives the fully-wired S3Client singleton.
     */
    @Bean
    public ApplicationRunner ensureBucketRunner(S3Client s3) {
        return args -> {
            try {
                s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
                log.info("Object storage bucket '{}' exists", bucket);
            } catch (NoSuchBucketException nsb) {
                log.info("Creating object storage bucket '{}'", bucket);
                try {
                    s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                } catch (BucketAlreadyOwnedByYouException ignored) {
                    // race condition, bucket was created between head and create
                }
            } catch (Exception e) {
                log.warn("Could not verify object storage bucket '{}' — startup continues (bucket will be created on first write): {}",
                        bucket, e.getMessage());
            }
        };
    }
}
