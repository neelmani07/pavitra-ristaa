package com.pavitraristaa.discovery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record CreateSavedSearchRequest(
        @NotBlank String name,
        @NotNull Map<String, Object> criteria,
        Boolean isDefault
) {
}
