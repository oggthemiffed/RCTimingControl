package dev.monkeypatch.rctiming.infrastructure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3ObjectStorageService implements ObjectStorageService {

    private final S3Client s3;
    private final String bucket;
    private final String publicBaseUrl;

    public S3ObjectStorageService(S3Client s3,
                                   @Value("${storage.bucket}") String bucket,
                                   @Value("${storage.publicBaseUrl}") String publicBaseUrl) {
        this.s3 = s3;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public String upload(String key, byte[] content, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentLength((long) content.length)
                .build();
        s3.putObject(request, RequestBody.fromBytes(content));
        return publicBaseUrl.replaceAll("/$", "") + "/" + key;
    }
}
