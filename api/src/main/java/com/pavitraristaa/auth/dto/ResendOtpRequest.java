package com.pavitraristaa.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ResendOtpRequest(
        @NotBlank String destination,
        @NotBlank String purpose
) {
}
