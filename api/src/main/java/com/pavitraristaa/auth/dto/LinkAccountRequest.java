package com.pavitraristaa.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LinkAccountRequest(@NotBlank String token) {
}
