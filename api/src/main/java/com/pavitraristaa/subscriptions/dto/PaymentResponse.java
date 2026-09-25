package com.pavitraristaa.subscriptions.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID subscriptionId,
        String provider,
        BigDecimal amount,
        String currencyCode,
        String status,
        String paymentMethod,
        Instant paidAt
) {
}
