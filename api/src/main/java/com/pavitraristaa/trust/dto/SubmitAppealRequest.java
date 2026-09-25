package com.pavitraristaa.trust.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmitAppealRequest(@NotBlank String type, @NotBlank String details) {
}
