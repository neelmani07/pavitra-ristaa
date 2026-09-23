package com.pavitraristaa.trust.dto;

import java.time.Instant;

/**
 * id is the internal numeric profile_verification id, matching GET /verification/{verificationId}'s own
 * "integer" typing in the API contract - profile_verification has no uuid column to expose instead.
 */
public record VerificationStatusResponse(
        Long id,
        String status,
        String verificationType,
        Instant verifiedAt,
        String notes
) {
}
