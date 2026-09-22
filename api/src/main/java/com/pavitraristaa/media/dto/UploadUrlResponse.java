package com.pavitraristaa.media.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record UploadUrlResponse(
        UUID mediaFileId,
        String uploadUrl,
        String method,
        Map<String, String> headers,
        Instant expiresAt
) {
}
