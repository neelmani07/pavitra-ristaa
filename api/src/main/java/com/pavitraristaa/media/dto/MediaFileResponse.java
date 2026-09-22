package com.pavitraristaa.media.dto;

import java.util.UUID;

public record MediaFileResponse(
        UUID id,
        String originalFilename,
        String mimeType,
        long fileSizeBytes,
        String url,
        String status
) {
}
