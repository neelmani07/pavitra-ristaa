package com.pavitraristaa.media.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.config.PavitraProperties;
import com.pavitraristaa.media.storage.ObjectStorage.PresignedUpload;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class S3ObjectStorageTest {

    private PavitraProperties properties;
    private PavitraProperties.Media media;

    @BeforeEach
    void setUp() {
        properties = new PavitraProperties();
        media = properties.getMedia();
        media.setBucket("pavitra-media");
        media.setEndpoint("http://localhost:9000");
        media.setAccessKey("test-access-key");
        media.setSecretKey("test-secret-key");
        media.setPathStyleAccess(true);
    }

    @Test
    void presignsUploadUrlWithoutContactingStorage() {
        PresignedUpload upload = new S3ObjectStorage(properties).createUploadUrl(
                "pavitra-media", "users/u1/general/m1.jpg", "image/jpeg", 2048, Duration.ofMinutes(10));

        assertThat(upload.url())
                .startsWith("http://localhost:9000/pavitra-media/users/u1/general/m1.jpg?")
                .contains("X-Amz-Signature=", "X-Amz-Expires=600");
        // Flexible checksums must not leak into the URL; browsers cannot satisfy them.
        assertThat(upload.url().toLowerCase()).doesNotContain("checksum");
        assertThat(upload.headers()).containsEntry("content-type", "image/jpeg");
        assertThat(upload.headers().keySet()).noneMatch(name -> name.equalsIgnoreCase("host"));
        assertThat(upload.expiresAt()).isAfter(java.time.Instant.now());
    }

    @Test
    void presignsDownloadUrl() {
        String url = new S3ObjectStorage(properties)
                .createDownloadUrl("pavitra-media", "users/u1/general/m1.jpg", Duration.ofHours(1));

        assertThat(url)
                .startsWith("http://localhost:9000/pavitra-media/users/u1/general/m1.jpg?")
                .contains("X-Amz-Signature=", "X-Amz-Expires=3600");
    }

    @Test
    void isConfiguredNeedsBucketAndKeysForCustomEndpoint() {
        assertThat(new S3ObjectStorage(properties).isConfigured()).isTrue();

        media.setSecretKey("");
        assertThat(new S3ObjectStorage(properties).isConfigured()).isFalse();

        media.setEndpoint("");
        assertThat(new S3ObjectStorage(properties).isConfigured()).isTrue();

        media.setBucket("");
        assertThat(new S3ObjectStorage(properties).isConfigured()).isFalse();
    }

    @Test
    void refusesToPresignWhenNotConfigured() {
        media.setBucket("");

        assertThatThrownBy(() -> new S3ObjectStorage(properties)
                .createUploadUrl("b", "k", "image/png", 1, Duration.ofMinutes(1)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Media storage is not configured");
    }
}
