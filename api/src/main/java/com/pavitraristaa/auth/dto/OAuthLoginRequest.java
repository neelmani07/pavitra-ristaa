package com.pavitraristaa.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthLoginRequest(
        @NotBlank String idToken,
        String deviceName,
        String platform
) {
}
