package com.pavitraristaa.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record SuspendUserRequest(@NotBlank String reason) {
}
