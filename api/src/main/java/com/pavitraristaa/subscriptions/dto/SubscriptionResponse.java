package com.pavitraristaa.subscriptions.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * checkoutSubscriptionId/checkoutKeyId are what the client passes to the provider's Checkout SDK (Razorpay
 * Checkout in "subscription" mode) to let the user authorize the recurring mandate - present only while
 * status is PENDING and the mandate hasn't been authorized yet; null once ACTIVE (nothing left to check out)
 * or for any other terminal status.
 */
public record SubscriptionResponse(
        UUID id,
        PlanResponse plan,
        String status,
        Instant startsAt,
        Instant endsAt,
        boolean autoRenew,
        String checkoutSubscriptionId,
        String checkoutKeyId
) {
}
