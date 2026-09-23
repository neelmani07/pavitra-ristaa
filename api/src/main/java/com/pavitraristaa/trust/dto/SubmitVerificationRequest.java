package com.pavitraristaa.trust.dto;

import java.util.List;
import java.util.UUID;

/**
 * mediaFileIds is accepted for contract compatibility but not stored: evidence storage for identity/selfie
 * verification is explicitly deferred in this project's docs (beyond profile_verification's own status fields).
 * This request only ever moves the status to PENDING.
 */
public record SubmitVerificationRequest(
        String verificationType,
        List<UUID> mediaFileIds
) {
}
