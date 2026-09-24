package com.pavitraristaa.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSupportTicketRequest(
        @NotBlank @Size(max = 50) String category,
        @NotBlank @Size(max = 200) String subject,
        @NotBlank String description,
        String priority
) {
}
