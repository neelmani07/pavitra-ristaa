package com.pavitraristaa.profile.dto;

public record UpdateLifestyleRequest(
        Long dietId,
        Long smokingId,
        Long drinkingId,
        Long exerciseFrequencyId,
        String sleepPattern,
        Boolean pets
) {
}
