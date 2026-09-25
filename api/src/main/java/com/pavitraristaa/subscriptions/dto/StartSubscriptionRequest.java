package com.pavitraristaa.subscriptions.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record StartSubscriptionRequest(
        @NotBlank String planCode,
        String couponCode,
        Map<String, Object> billingAddress
) {
}
