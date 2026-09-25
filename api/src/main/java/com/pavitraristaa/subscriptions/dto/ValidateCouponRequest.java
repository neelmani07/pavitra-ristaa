package com.pavitraristaa.subscriptions.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateCouponRequest(@NotBlank String couponCode, @NotBlank String planCode) {
}
