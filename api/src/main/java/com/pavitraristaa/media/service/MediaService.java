package com.pavitraristaa.media.service;

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
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Upload flow: request URL (row created as UPLOADING) -> client uploads straight to storage -> complete (row ACTIVE).
 */
@Service
public class MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaService.class);
    private static final String DEFAULT_PURPOSE = "general";
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final AuthService authService;
    private final MediaFileRepository mediaFileRepository;
    private final List<MediaUsageChecker> usageCheckers;
    private final ObjectStorage objectStorage;
    private final MediaUrlResolver mediaUrlResolver;
    private final PavitraProperties.Media config;

    public MediaService(
            AuthService authService,
            MediaFileRepository mediaFileRepository,
            List<MediaUsageChecker> usageCheckers,
            ObjectStorage objectStorage,
            MediaUrlResolver mediaUrlResolver,
            PavitraProperties properties
    ) {
        this.authService = authService;
        this.mediaFileRepository = mediaFileRepository;
        this.usageCheckers = usageCheckers;
        this.objectStorage = objectStorage;
        this.mediaUrlResolver = mediaUrlResolver;
        this.config = properties.getMedia();
    }

    @Transactional
    public UploadUrlResponse createUploadUrl(AuthenticatedUser principal, CreateUploadUrlRequest request) {
        UserAccount user = authService.requireUsable(principal);
        String mimeType = request.mimeType().trim().toLowerCase(Locale.ROOT);
        if (!config.getAllowedMimeTypes().contains(mimeType)) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "Unsupported file type",
                    Map.of("mimeType", mimeType, "allowed", config.getAllowedMimeTypes())
            );
        }
        long size = request.fileSizeBytes();
        if (size > config.getMaxFileSizeBytes()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "File is too large",
                    Map.of("maxFileSizeBytes", config.getMaxFileSizeBytes())
            );
        }
        if (!objectStorage.isConfigured()) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Media storage is not configured");
        }

        UUID mediaId = UUID.randomUUID();
        MediaFile media = new MediaFile();
        media.setUuid(mediaId);
        media.setOwner(user);
        media.setStorageProvider(StorageProvider.valueOf(config.getProvider().name()));
        media.setBucket(config.getBucket());
        media.setObjectKey("users/" + user.getUuid() + "/" + purposeSegment(request.purpose()) + "/"
                + mediaId + EXTENSIONS.getOrDefault(mimeType, ""));
        media.setOriginalFilename(sanitizeFilename(request.filename()));
        media.setMimeType(mimeType);
        media.setFileSizeBytes(size);
        media.setStatus(MediaStatus.UPLOADING);
        media.setCreatedAt(Instant.now());

        PresignedUpload upload = objectStorage.createUploadUrl(
                media.getBucket(), media.getObjectKey(), mimeType, size, config.getUploadUrlTtl());
        mediaFileRepository.save(media);
        return new UploadUrlResponse(mediaId, upload.url(), "PUT", upload.headers(), upload.expiresAt());
    }

    /** Keeps the rejection persisted (row marked DELETED) even though an ApiException is thrown. */
    @Transactional(noRollbackFor = ApiException.class)
    public MediaFileResponse complete(AuthenticatedUser principal, CompleteUploadRequest request) {
        UserAccount user = authService.requireUsable(principal);
        MediaFile media = findOwned(request.mediaFileId(), user);
        if (media.getStatus() == MediaStatus.ACTIVE) {
            return toResponse(media);
        }
        if (media.getStatus() != MediaStatus.UPLOADING) {
            throw mediaNotFound();
        }

        StoredObject stored = objectStorage.find(media.getBucket(), media.getObjectKey())
                .orElseThrow(() -> new ApiException(
                        ErrorCode.VALIDATION_ERROR,
                        "Upload not found in storage; upload the file before completing",
                        Map.of("mediaFileId", media.getUuid())
                ));
        if (stored.sizeBytes() != media.getFileSizeBytes() || !sameType(stored.contentType(), media.getMimeType())) {
            media.setStatus(MediaStatus.DELETED);
            media.setDeletedAt(Instant.now());
            mediaFileRepository.save(media);
            deleteObjectQuietly(media);
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "Uploaded file does not match the requested upload",
                    Map.of("mediaFileId", media.getUuid())
            );
        }
        if (request.checksum() != null && !request.checksum().isBlank()) {
            media.setChecksum(request.checksum().trim());
        }
        media.setStatus(MediaStatus.ACTIVE);
        return toResponse(mediaFileRepository.save(media));
    }

    public void delete(AuthenticatedUser principal, UUID mediaId) {
        UserAccount user = authService.requireUsable(principal);
        MediaFile media = findOwned(mediaId, user);
        if (usageCheckers.stream().anyMatch(checker -> checker.isInUse(media))) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "Media is still in use; remove it from where it is used first",
                    Map.of("mediaFileId", mediaId)
            );
        }
        media.setStatus(MediaStatus.DELETED);
        media.setDeletedAt(Instant.now());
        mediaFileRepository.save(media);
        deleteObjectQuietly(media);
    }

    private MediaFile findOwned(UUID mediaId, UserAccount owner) {
        return mediaFileRepository.findByUuidAndOwnerAndStatusNot(mediaId, owner, MediaStatus.DELETED)
                .orElseThrow(this::mediaNotFound);
    }

    private ApiException mediaNotFound() {
        return new ApiException(ErrorCode.MEDIA_NOT_FOUND, "Media file not found");
    }

    private MediaFileResponse toResponse(MediaFile media) {
        return new MediaFileResponse(
                media.getUuid(),
                media.getOriginalFilename(),
                media.getMimeType(),
                media.getFileSizeBytes(),
                mediaUrlResolver.urlFor(media),
                media.getStatus().name()
        );
    }

    private void deleteObjectQuietly(MediaFile media) {
        try {
            objectStorage.delete(media.getBucket(), media.getObjectKey());
        } catch (RuntimeException exception) {
            log.warn("Could not remove stored object for media {}", media.getUuid());
        }
    }

    private static boolean sameType(String actual, String expected) {
        if (actual == null) {
            return false;
        }
        String base = actual.split(";", 2)[0].trim();
        return base.equalsIgnoreCase(expected);
    }

    private static String purposeSegment(String purpose) {
        if (purpose == null) {
            return DEFAULT_PURPOSE;
        }
        String cleaned = purpose.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("^-+|-+$", "");
        return cleaned.isEmpty() ? DEFAULT_PURPOSE : cleaned;
    }

    private static String sanitizeFilename(String filename) {
        String name = filename.trim().replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "");
        return name.isEmpty() ? null : name;
    }
}
