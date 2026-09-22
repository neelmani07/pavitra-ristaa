package com.pavitraristaa.profile.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record UpdateCareerRequest(
        Long occupationId,
        String jobTitle,
        String companyName,
        Long industryId,
        Long workLocationCityId,
        BigDecimal experienceYears,
        @JsonProperty("isEmployed") Boolean employed
) {
}
