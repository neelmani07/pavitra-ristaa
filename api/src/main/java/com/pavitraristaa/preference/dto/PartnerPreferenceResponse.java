package com.pavitraristaa.preference.dto;

import java.math.BigDecimal;
import java.util.List;

public record PartnerPreferenceResponse(
        Integer minAge,
        Integer maxAge,
        Integer minHeightCm,
        Integer maxHeightCm,
        String preferredGender,
        BigDecimal minIncome,
        BigDecimal maxIncome,
        String currencyCode,
        Long preferredMaritalStatusId,
        Integer locationRadiusKm,
        List<PreferenceValueResponse> values
) {
}
