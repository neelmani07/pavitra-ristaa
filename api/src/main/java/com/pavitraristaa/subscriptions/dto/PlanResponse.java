package com.pavitraristaa.subscriptions.dto;

import java.math.BigDecimal;
import java.util.Map;

public record PlanResponse(
        String code,
        String name,
        String description,
        BigDecimal price,
        String currencyCode,
        String billingPeriod,
        int durationDays,
        Map<String, Object> features
) {
}
