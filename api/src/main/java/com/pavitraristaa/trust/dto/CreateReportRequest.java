package com.pavitraristaa.trust.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Exactly one of reportedUserId/reportedMessageId should normally be set, but neither is required by the
 * contract, so both stay optional and only reasonId is enforced. */
public record CreateReportRequest(
        UUID reportedUserId,
        UUID reportedMessageId,
        @NotNull Long reasonId,
        String details
) {
}
