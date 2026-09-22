package com.pavitraristaa.preference.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PreferenceValueRequest(
        @NotBlank String preferenceType,
        @NotNull Long masterValueId
) {
}
