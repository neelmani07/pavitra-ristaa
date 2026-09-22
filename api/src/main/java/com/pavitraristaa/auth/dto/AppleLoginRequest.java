package com.pavitraristaa.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AppleLoginRequest(
        @NotBlank String identityToken,
        String authorizationCode,
        String deviceName,
        String platform
) {
}
