package com.pavitraristaa.profile.dto;

import java.time.LocalDate;

/**
 * Location can be sent either as master-data ids (countryId/stateId/cityId, from our own dropdown) or as plain
 * names (country/state/city, from whatever source the frontend's dropdowns use). A name wins over an id for the
 * same level; either way the profile stores the master-data row, which is created on first use for a new name.
 */
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
        Long cityId,
        String country,
        String state,
        String city
) {
}
