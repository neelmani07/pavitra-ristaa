package com.pavitraristaa.subscriptions.dto;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        PlanResponse plan,
        String status,
        Instant startsAt,
        Instant endsAt,
        boolean autoRenew
) {
}
