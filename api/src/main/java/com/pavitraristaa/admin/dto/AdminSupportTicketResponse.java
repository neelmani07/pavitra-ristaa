package com.pavitraristaa.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminSupportTicketResponse(
        UUID id,
        UUID userId,
        String category,
        String subject,
        String description,
        String status,
        String priority,
        UUID assignedToId,
        Instant resolvedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
