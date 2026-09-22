package com.pavitraristaa.profile.dto;

import java.time.Instant;

public record SpiritualProfileResponse(
        String spiritualCommunity,
        String spiritualInterests,
        String practices,
        String anySpiritualProfession,
        String dreamSpiritualPilgrimageDestination,
        Instant createdAt,
        Instant updatedAt
) {
}
