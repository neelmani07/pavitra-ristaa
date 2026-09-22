package com.pavitraristaa.profile.dto;

import java.time.LocalDate;

public record UpdateProfileRequest(
        String firstName,
        String lastName,
        String displayName,
        LocalDate dateOfBirth,
        String gender,
        String headline,
        String aboutMe,
        Long countryId,
        Long stateId,
        Long cityId
) {
}
