package com.pavitraristaa.admin.dto;

import jakarta.validation.constraints.NotBlank;

/** status must be APPROVED or REJECTED. */
public record ResolveAppealRequest(@NotBlank String status, String resolutionNotes) {
}
