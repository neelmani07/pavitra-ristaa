package com.pavitraristaa.discovery.dto;

import java.time.Instant;
import java.util.Map;

public record SavedSearchResponse(
        Long id,
        String name,
        Map<String, Object> criteria,
        boolean isDefault,
        Instant createdAt,
        Instant updatedAt
) {
}
