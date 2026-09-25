package com.pavitraristaa.subscriptions.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.subscriptions.dto.PaymentResponse;
import com.pavitraristaa.subscriptions.dto.SubscriptionResponse;
import com.pavitraristaa.subscriptions.service.PaymentService;
import com.pavitraristaa.subscriptions.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Subscription & Payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final SubscriptionService subscriptionService;
    private final CurrentUserAccessor currentUserAccessor;

    public PaymentController(PaymentService paymentService, SubscriptionService subscriptionService, CurrentUserAccessor currentUserAccessor) {
        this.paymentService = paymentService;
        this.subscriptionService = subscriptionService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping
    @Operation(summary = "List payment history")
    public ApiResponse<List<PaymentResponse>> list(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(paymentService.listMine(currentUserAccessor.requireUser(), page, size), "Payments");
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment status/details")
    public ApiResponse<PaymentResponse> getOne(@PathVariable UUID paymentId) {
        return ApiResponse.ok(paymentService.getOne(currentUserAccessor.requireUser(), paymentId), "Payment");
    }

    @PostMapping("/{paymentId}/retry")
    @Operation(summary = "Retry failed payment - only a failed first-payment attempt on a still-pending "
            + "subscription; returns a fresh checkout session to complete")
    public ApiResponse<SubscriptionResponse> retry(@PathVariable UUID paymentId) {
        return ApiResponse.ok(subscriptionService.retryFailedCheckout(currentUserAccessor.requireUser(), paymentId), "Checkout retried");
    }
}
