package com.pavitraristaa.preference.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** Replaces the whole preference set: omitted scalar fields are cleared, and {@code values} is the full list. */
public record PartnerPreferenceRequest(
        @Min(0) @Max(120) Integer minAge,
        @Min(0) @Max(120) Integer maxAge,
        @Min(50) @Max(300) Integer minHeightCm,
        @Min(50) @Max(300) Integer maxHeightCm,
        @Size(max = 30) String preferredGender,
        @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal minIncome,
        @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal maxIncome,
        @Pattern(regexp = "[A-Za-z]{3}") String currencyCode,
        Long preferredMaritalStatusId,
        @Min(1) @Max(20000) Integer locationRadiusKm,
        @Valid List<@Valid PreferenceValueRequest> values
) {
}
