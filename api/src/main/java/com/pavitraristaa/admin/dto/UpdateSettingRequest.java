package com.pavitraristaa.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateSettingRequest(@NotBlank String value) {
}
