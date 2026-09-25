package com.pavitraristaa.subscriptions.dto;

import java.math.BigDecimal;

public record CouponValidationResponse(
        boolean valid,
        String couponCode,
        String discountType,
        BigDecimal discountValue,
        BigDecimal originalPrice,
        BigDecimal discountedPrice,
        String reason
) {
}
