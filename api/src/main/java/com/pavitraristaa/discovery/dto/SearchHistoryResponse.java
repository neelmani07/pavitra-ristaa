package com.pavitraristaa.discovery.dto;

import java.time.Instant;
import java.util.Map;

public record SearchHistoryResponse(
        Long id,
        Map<String, Object> criteria,
        Instant searchedAt
) {
}
