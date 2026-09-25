package com.pavitraristaa.subscriptions.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.subscriptions.dto.CouponValidationResponse;
import com.pavitraristaa.subscriptions.dto.ValidateCouponRequest;
import com.pavitraristaa.subscriptions.service.CouponService;
import com.pavitraristaa.subscriptions.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coupons")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Subscription & Payments")
public class CouponController {

    private final CouponService couponService;
    private final PlanService planService;

    public CouponController(CouponService couponService, PlanService planService) {
        this.couponService = couponService;
        this.planService = planService;
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate a coupon")
    public ApiResponse<CouponValidationResponse> validate(@Valid @RequestBody ValidateCouponRequest request) {
        BigDecimal price = planService.priceOf(request.planCode());
        return ApiResponse.ok(couponService.validate(request, price), "Coupon validation");
    }
}
