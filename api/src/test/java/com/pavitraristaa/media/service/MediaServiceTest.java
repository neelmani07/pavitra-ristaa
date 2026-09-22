package com.pavitraristaa.media.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pavitraristaa.auth.entity.AccountStatus;
import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.config.PavitraProperties;
import com.pavitraristaa.media.dto.CompleteUploadRequest;
import com.pavitraristaa.media.dto.CreateUploadUrlRequest;
import com.pavitraristaa.media.dto.MediaFileResponse;
import com.pavitraristaa.media.dto.UploadUrlResponse;
import com.pavitraristaa.media.entity.MediaFile;
import com.pavitraristaa.media.entity.MediaStatus;
import com.pavitraristaa.media.entity.StorageProvider;
import com.pavitraristaa.media.repository.MediaFileRepository;
import com.pavitraristaa.media.storage.ObjectStorage.PresignedUpload;
import com.pavitraristaa.media.storage.ObjectStorage.StoredObject;
import com.pavitraristaa.media.storage.ObjectStorage;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock private AuthService authService;
    @Mock private MediaFileRepository mediaFileRepository;
    @Mock private MediaUsageChecker usageChecker;
    @Mock private ObjectStorage objectStorage;
    @Mock private MediaUrlResolver mediaUrlResolver;

    private PavitraProperties properties;
    private MediaService mediaService;
    private UserAccount user;
    private AuthenticatedUser principal;

    @BeforeEach
    void setUp() {
        properties = new PavitraProperties();
        properties.getMedia().setBucket("pavitra-media");
        properties.getMedia().setProvider(PavitraProperties.Media.Provider.MINIO);
        mediaService = new MediaService(
                authService, mediaFileRepository, List.of(usageChecker), objectStorage, mediaUrlResolver, properties);
        user = new UserAccount();
        user.setId(7L);
        user.setUuid(UUID.randomUUID());
        user.setAccountStatus(AccountStatus.ACTIVE);
        principal = new AuthenticatedUser(user.getId(), user.getUuid(), List.of("USER"), 1L);
        when(authService.requireUsable(principal)).thenReturn(user);
    }

    @Test
    void createUploadUrlStoresUploadingRowAndReturnsPresignedPut() {
        when(objectStorage.isConfigured()).thenReturn(true);
        Instant expiry = Instant.now().plusSeconds(600);
        when(objectStorage.createUploadUrl(eq("pavitra-media"), any(), eq("image/jpeg"), eq(2048L), any(Duration.class)))
                .thenReturn(new PresignedUpload("https://storage/put", Map.of("Content-Type", "image/jpeg"), expiry));

        UploadUrlResponse response = mediaService.createUploadUrl(
                principal, new CreateUploadUrlRequest("../me.JPG", "IMAGE/JPEG", 2048L, "Profile Photo!"));

        ArgumentCaptor<MediaFile> saved = ArgumentCaptor.forClass(MediaFile.class);
        verify(mediaFileRepository).save(saved.capture());
        MediaFile media = saved.getValue();
        assertThat(media.getStatus()).isEqualTo(MediaStatus.UPLOADING);
        assertThat(media.getOwner()).isSameAs(user);
        assertThat(media.getStorageProvider()).isEqualTo(StorageProvider.MINIO);
        assertThat(media.getMimeType()).isEqualTo("image/jpeg");
        assertThat(media.getOriginalFilename()).isEqualTo("me.JPG");
        assertThat(media.getObjectKey())
                .isEqualTo("users/" + user.getUuid() + "/profile-photo/" + media.getUuid() + ".jpg");
        assertThat(response.mediaFileId()).isEqualTo(media.getUuid());
        assertThat(response.uploadUrl()).isEqualTo("https://storage/put");
        assertThat(response.method()).isEqualTo("PUT");
        assertThat(response.expiresAt()).isEqualTo(expiry);
    }

    @Test
    void createUploadUrlRejectsUnsupportedType() {
        assertThatThrownBy(() -> mediaService.createUploadUrl(
                principal, new CreateUploadUrlRequest("a.exe", "application/x-msdownload", 100L, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        verify(mediaFileRepository, never()).save(any());
    }

    @Test
    void createUploadUrlRejectsOversizedFile() {
        long tooBig = properties.getMedia().getMaxFileSizeBytes() + 1;

        assertThatThrownBy(() -> mediaService.createUploadUrl(
                principal, new CreateUploadUrlRequest("a.png", "image/png", tooBig, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        verify(mediaFileRepository, never()).save(any());
    }

    @Test
    void completeActivatesMediaWhenStoredObjectMatches() {
        MediaFile media = uploading();
        when(mediaFileRepository.findByUuidAndOwnerAndStatusNot(media.getUuid(), user, MediaStatus.DELETED))
                .thenReturn(Optional.of(media));
        when(objectStorage.find(media.getBucket(), media.getObjectKey()))
                .thenReturn(Optional.of(new StoredObject(2048, "image/jpeg")));
        when(mediaFileRepository.save(media)).thenReturn(media);
        when(mediaUrlResolver.urlFor(media)).thenReturn("https://storage/get");

        MediaFileResponse response = mediaService.complete(
                principal, new CompleteUploadRequest(media.getUuid(), " abc123 "));

        assertThat(media.getStatus()).isEqualTo(MediaStatus.ACTIVE);
        assertThat(media.getChecksum()).isEqualTo("abc123");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.url()).isEqualTo("https://storage/get");
    }

    @Test
    void completeIsIdempotentForActiveMedia() {
        MediaFile media = uploading();
        media.setStatus(MediaStatus.ACTIVE);
        when(mediaFileRepository.findByUuidAndOwnerAndStatusNot(media.getUuid(), user, MediaStatus.DELETED))
                .thenReturn(Optional.of(media));

        MediaFileResponse response = mediaService.complete(principal, new CompleteUploadRequest(media.getUuid(), null));

        assertThat(response.status()).isEqualTo("ACTIVE");
        verify(objectStorage, never()).find(any(), any());
    }

    @Test
    void completeFailsWhenNothingWasUploaded() {
        MediaFile media = uploading();
        when(mediaFileRepository.findByUuidAndOwnerAndStatusNot(media.getUuid(), user, MediaStatus.DELETED))
                .thenReturn(Optional.of(media));
        when(objectStorage.find(media.getBucket(), media.getObjectKey())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mediaService.complete(principal, new CompleteUploadRequest(media.getUuid(), null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        assertThat(media.getStatus()).isEqualTo(MediaStatus.UPLOADING);
    }

    @Test
    void completeRejectsAndDiscardsMismatchedUpload() {
        MediaFile media = uploading();
        when(mediaFileRepository.findByUuidAndOwnerAndStatusNot(media.getUuid(), user, MediaStatus.DELETED))
                .thenReturn(Optional.of(media));
        when(objectStorage.find(media.getBucket(), media.getObjectKey()))
                .thenReturn(Optional.of(new StoredObject(999_999, "image/jpeg")));

        assertThatThrownBy(() -> mediaService.complete(principal, new CompleteUploadRequest(media.getUuid(), null)))
                .isInstanceOf(ApiException.class);

        assertThat(media.getStatus()).isEqualTo(MediaStatus.DELETED);
        verify(mediaFileRepository).save(media);
        verify(objectStorage).delete(media.getBucket(), media.getObjectKey());
    }

    @Test
    void completeReturnsNotFoundForUnknownMedia() {
        UUID unknown = UUID.randomUUID();
        when(mediaFileRepository.findByUuidAndOwnerAndStatusNot(unknown, user, MediaStatus.DELETED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> mediaService.complete(principal, new CompleteUploadRequest(unknown, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEDIA_NOT_FOUND));
    }

    @Test
    void deleteRefusesMediaUsedByProfilePhoto() {
        MediaFile media = uploading();
        media.setStatus(MediaStatus.ACTIVE);
        when(mediaFileRepository.findByUuidAndOwnerAndStatusNot(media.getUuid(), user, MediaStatus.DELETED))
                .thenReturn(Optional.of(media));
        when(usageChecker.isInUse(media)).thenReturn(true);

        assertThatThrownBy(() -> mediaService.delete(principal, media.getUuid()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        assertThat(media.getStatus()).isEqualTo(MediaStatus.ACTIVE);
        verify(objectStorage, never()).delete(any(), any());
    }

    @Test
    void deleteMarksDeletedAndRemovesObject() {
        MediaFile media = uploading();
        media.setStatus(MediaStatus.ACTIVE);
        when(mediaFileRepository.findByUuidAndOwnerAndStatusNot(media.getUuid(), user, MediaStatus.DELETED))
                .thenReturn(Optional.of(media));
        when(usageChecker.isInUse(media)).thenReturn(false);

        mediaService.delete(principal, media.getUuid());

        assertThat(media.getStatus()).isEqualTo(MediaStatus.DELETED);
        assertThat(media.getDeletedAt()).isNotNull();
        verify(mediaFileRepository).save(media);
        verify(objectStorage).delete(media.getBucket(), media.getObjectKey());
    }

    private MediaFile uploading() {
        MediaFile media = new MediaFile();
        media.setUuid(UUID.randomUUID());
        media.setOwner(user);
        media.setStorageProvider(StorageProvider.MINIO);
        media.setBucket("pavitra-media");
        media.setObjectKey("users/" + user.getUuid() + "/general/" + media.getUuid() + ".jpg");
        media.setMimeType("image/jpeg");
        media.setFileSizeBytes(2048);
        media.setStatus(MediaStatus.UPLOADING);
        media.setCreatedAt(Instant.now());
        return media;
    }
}
