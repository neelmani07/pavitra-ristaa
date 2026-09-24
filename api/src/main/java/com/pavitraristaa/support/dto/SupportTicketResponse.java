package com.pavitraristaa.support.dto;

import java.time.Instant;
import java.util.UUID;

public record SupportTicketResponse(
        UUID id,
        String category,
        String subject,
        String description,
        String status,
        String priority,
        Instant resolvedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
