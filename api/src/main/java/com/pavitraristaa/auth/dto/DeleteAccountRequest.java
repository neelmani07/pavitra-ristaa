package com.pavitraristaa.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(
        @NotBlank String reason,
        @NotBlank String confirmation
) {
}
