package com.pavitraristaa.connections.dto;

import java.util.List;
import java.util.UUID;

/**
 * A first-pass heuristic score (0-100), not a machine-learned model. Every criterion is skipped, not penalized,
 * when either side's relevant data is missing - matching the product rule that absent optional information
 * (including spiritual information) must never count as a negative signal. Uses the same criteria regardless of
 * relationship mode: Dating, Friendship and Marriage share one compatibility model.
 */
public record CompatibilityResponse(
        UUID matchId,
        int score,
        List<CompatibilityFactor> factors
) {
    public record CompatibilityFactor(String key, String label, boolean matched, String detail) {
    }
}
