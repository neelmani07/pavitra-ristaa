package com.pavitraristaa.media.storage;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Object storage used for media content. The database only stores metadata; content never passes through the API.
 */
public interface ObjectStorage {

    boolean isConfigured();

    PresignedUpload createUploadUrl(String bucket, String objectKey, String contentType, long contentLength, Duration ttl);

    String createDownloadUrl(String bucket, String objectKey, Duration ttl);

    Optional<StoredObject> find(String bucket, String objectKey);

    void delete(String bucket, String objectKey);

    record PresignedUpload(String url, Map<String, String> headers, Instant expiresAt) {
    }

    record StoredObject(long sizeBytes, String contentType) {
    }
}
