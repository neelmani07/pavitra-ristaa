package com.pavitraristaa.admin.dto;

import java.time.Instant;

public record AdminSettingResponse(
        String key,
        String value,
        String valueType,
        String description,
        boolean isPublic,
        Instant updatedAt
) {
}
