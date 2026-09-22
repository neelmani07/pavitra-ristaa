package com.pavitraristaa.media.storage;

import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.config.PavitraProperties;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * S3 / MinIO implementation. Clients are created lazily so the application starts without storage configured.
 */
@Component
public class S3ObjectStorage implements ObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(S3ObjectStorage.class);
    private static final List<String> CLIENT_MANAGED_HEADERS = List.of("host", "content-length");

    private final PavitraProperties.Media config;
    private S3Client client;
    private S3Presigner presigner;

    public S3ObjectStorage(PavitraProperties properties) {
        this.config = properties.getMedia();
    }

    @Override
    public boolean isConfigured() {
        if (isBlank(config.getBucket())) {
            return false;
        }
        boolean hasKeys = !isBlank(config.getAccessKey()) && !isBlank(config.getSecretKey());
        // A custom endpoint (MinIO) needs explicit keys; plain S3 can fall back to the default AWS credential chain.
        return hasKeys || isBlank(config.getEndpoint());
    }

    @Override
    public PresignedUpload createUploadUrl(
            String bucket, String objectKey, String contentType, long contentLength, Duration ttl
    ) {
        try {
            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();
            PresignedPutObjectRequest presigned = presigner().presignPutObject(PutObjectPresignRequest.builder()
                    .signatureDuration(ttl)
                    .putObjectRequest(put)
                    .build());
            Map<String, String> headers = new LinkedHashMap<>();
            presigned.signedHeaders().forEach((name, values) -> {
                if (!CLIENT_MANAGED_HEADERS.contains(name.toLowerCase())) {
                    headers.put(name, String.join(",", values));
                }
            });
            return new PresignedUpload(presigned.url().toString(), headers, Instant.now().plus(ttl));
        } catch (SdkException exception) {
            throw storageFailure("create upload URL", exception);
        }
    }

    @Override
    public String createDownloadUrl(String bucket, String objectKey, Duration ttl) {
        try {
            return presigner().presignGetObject(GetObjectPresignRequest.builder()
                    .signatureDuration(ttl)
                    .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(objectKey).build())
                    .build()).url().toString();
        } catch (SdkException exception) {
            throw storageFailure("create download URL", exception);
        }
    }

    @Override
    public Optional<StoredObject> find(String bucket, String objectKey) {
        try {
            var head = client().headObject(HeadObjectRequest.builder().bucket(bucket).key(objectKey).build());
            return Optional.of(new StoredObject(head.contentLength(), head.contentType()));
        } catch (NoSuchKeyException exception) {
            return Optional.empty();
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return Optional.empty();
            }
            throw storageFailure("check object", exception);
        } catch (SdkException exception) {
            throw storageFailure("check object", exception);
        }
    }

    @Override
    public void delete(String bucket, String objectKey) {
        try {
            client().deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
        } catch (SdkException exception) {
            throw storageFailure("delete object", exception);
        }
    }

    @PreDestroy
    synchronized void close() {
        if (client != null) {
            client.close();
        }
        if (presigner != null) {
            presigner.close();
        }
    }

    private synchronized S3Client client() {
        requireConfigured();
        if (client == null) {
            var builder = S3Client.builder()
                    .region(Region.of(config.getRegion()))
                    .credentialsProvider(credentials())
                    .forcePathStyle(config.isPathStyleAccess())
                    .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                    .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);
            if (!isBlank(config.getEndpoint())) {
                builder.endpointOverride(URI.create(config.getEndpoint()));
            }
            client = builder.build();
        }
        return client;
    }

    private synchronized S3Presigner presigner() {
        requireConfigured();
        if (presigner == null) {
            var builder = S3Presigner.builder()
                    .region(Region.of(config.getRegion()))
                    .credentialsProvider(credentials())
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(config.isPathStyleAccess())
                            .build());
            if (!isBlank(config.getEndpoint())) {
                builder.endpointOverride(URI.create(config.getEndpoint()));
            }
            presigner = builder.build();
        }
        return presigner;
    }

    private AwsCredentialsProvider credentials() {
        if (!isBlank(config.getAccessKey()) && !isBlank(config.getSecretKey())) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(config.getAccessKey(), config.getSecretKey()));
        }
        return DefaultCredentialsProvider.builder().build();
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Media storage is not configured");
        }
    }

    private ApiException storageFailure(String action, SdkException exception) {
        log.error("Media storage failed to {}: {}", action, exception.getMessage());
        return new ApiException(ErrorCode.INTERNAL_ERROR, "Media storage is unavailable");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
