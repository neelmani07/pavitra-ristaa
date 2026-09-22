package com.pavitraristaa.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyOtpRequest(
        @NotBlank String destination,
        @NotBlank String otp,
        @NotBlank String purpose
) {
}
