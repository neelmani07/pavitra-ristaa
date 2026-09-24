package com.pavitraristaa.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateModerationRequest(
        @NotNull UUID targetUserId,
        @NotBlank String action,
        String reason
) {
}
