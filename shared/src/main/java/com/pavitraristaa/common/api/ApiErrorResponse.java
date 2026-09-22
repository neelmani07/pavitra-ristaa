package com.pavitraristaa.common.api;

import java.time.Instant;

public record ApiErrorResponse(boolean success, ApiError error, Instant timestamp) {

    public static ApiErrorResponse of(ApiError error) {
        return new ApiErrorResponse(false, error, Instant.now());
    }
}
