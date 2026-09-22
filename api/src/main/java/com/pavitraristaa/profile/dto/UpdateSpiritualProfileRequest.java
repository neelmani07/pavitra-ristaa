package com.pavitraristaa.profile.dto;

public record UpdateSpiritualProfileRequest(
        String spiritualCommunity,
        String spiritualInterests,
        String practices,
        String anySpiritualProfession,
        String dreamSpiritualPilgrimageDestination
) {
}
