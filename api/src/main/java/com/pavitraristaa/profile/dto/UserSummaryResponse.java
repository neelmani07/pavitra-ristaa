package com.pavitraristaa.profile.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Lightweight profile card used by discovery, favorites, interests and matches - never the full profile. */
public record UserSummaryResponse(
        UUID id,
        String displayName,
        Integer age,
        String gender,
        Map<String, Object> location,
        String primaryPhoto,
        List<String> relationshipModes,
        boolean verified
) {
}
