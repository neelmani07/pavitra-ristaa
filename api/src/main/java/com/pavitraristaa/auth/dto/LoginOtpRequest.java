package com.pavitraristaa.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginOtpRequest(
        @NotBlank String mobile,
        @NotBlank String otp,
        String deviceName,
        String platform
) {
}
