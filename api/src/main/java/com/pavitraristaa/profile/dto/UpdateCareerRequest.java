package com.pavitraristaa.profile.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record UpdateCareerRequest(
        Long occupationId,
        String jobTitle,
        String companyName,
        Long industryId,
        Long workLocationCityId,
        /** Plain-name alternative to workLocationCityId; wins over it when both are sent. */
        String workLocationCity,
        BigDecimal experienceYears,
        @JsonProperty("isEmployed") Boolean employed,
        /** https://www.linkedin.com/in/... Owner-only: never returned to anyone else viewing the profile. */
        String linkedinUrl
) {
}
