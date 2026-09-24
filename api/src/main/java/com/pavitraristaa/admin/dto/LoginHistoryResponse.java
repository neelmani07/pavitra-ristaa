package com.pavitraristaa.admin.dto;

import java.time.Instant;

public record LoginHistoryResponse(
        Instant loginAt,
        String loginType,
        boolean success,
        String ipAddress,
        String userAgent,
        String failureReason
) {
}
