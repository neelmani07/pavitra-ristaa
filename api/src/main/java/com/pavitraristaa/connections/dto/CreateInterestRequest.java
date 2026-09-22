package com.pavitraristaa.connections.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateInterestRequest(
        @NotNull UUID profileId,
        @NotBlank String relationshipMode,
        @Size(max = 500) String message
) {
}
