package com.pavitraristaa.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * status must be RESOLVED or DISMISSED. When a punitive moderationAction is also given, resolving the report
 * creates a Moderation row (source_report_id = this report) against the reported user in the same transaction.
 */
public record ResolveReportRequest(
        @NotBlank String status,
        String moderationAction,
        String moderationReason
) {
}
