package com.pavitraristaa.discovery.dto;

import com.pavitraristaa.profile.dto.UserSummaryResponse;
import java.util.List;

/**
 * Deliberately modest for this first pass: profile completion plus a small slice of discoverable profiles.
 * Richer aggregation (unread messages, new-match badges, notification counts) naturally follows once messaging
 * and notifications exist - it isn't invented here ahead of those modules.
 */
public record HomeResponse(
        int profileCompletionPercent,
        List<String> missingProfileSections,
        List<UserSummaryResponse> discoverProfiles
) {
}
