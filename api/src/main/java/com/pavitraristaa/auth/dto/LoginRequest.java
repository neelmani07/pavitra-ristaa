package com.pavitraristaa.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String identifier,
        @NotBlank String password,
        Boolean rememberMe,
        String deviceName,
        String platform
) {
}
