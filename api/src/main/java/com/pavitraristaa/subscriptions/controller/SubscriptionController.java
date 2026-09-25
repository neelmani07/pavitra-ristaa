package com.pavitraristaa.subscriptions.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.subscriptions.dto.StartSubscriptionRequest;
import com.pavitraristaa.subscriptions.dto.SubscriptionResponse;
import com.pavitraristaa.subscriptions.dto.UpdateAutoRenewRequest;
import com.pavitraristaa.subscriptions.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscriptions")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Subscription & Payments")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final CurrentUserAccessor currentUserAccessor;

    public SubscriptionController(SubscriptionService subscriptionService, CurrentUserAccessor currentUserAccessor) {
        this.subscriptionService = subscriptionService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current subscription")
    public ApiResponse<SubscriptionResponse> current() {
        return ApiResponse.ok(subscriptionService.getCurrent(currentUserAccessor.requireUser()), "Current subscription");
    }

    @PostMapping
    @Operation(summary = "Start subscription checkout")
    public ApiResponse<SubscriptionResponse> start(@Valid @RequestBody StartSubscriptionRequest request) {
        return ApiResponse.ok(subscriptionService.start(currentUserAccessor.requireUser(), request), "Subscription started");
    }

    @GetMapping("/{subscriptionId}")
    @Operation(summary = "Get subscription details")
    public ApiResponse<SubscriptionResponse> getOne(@PathVariable UUID subscriptionId) {
        return ApiResponse.ok(subscriptionService.getOne(currentUserAccessor.requireUser(), subscriptionId), "Subscription");
    }

    @PostMapping("/{subscriptionId}/cancel")
    @Operation(summary = "Cancel subscription")
    public ApiResponse<SubscriptionResponse> cancel(@PathVariable UUID subscriptionId) {
        return ApiResponse.ok(subscriptionService.cancel(currentUserAccessor.requireUser(), subscriptionId), "Subscription cancelled");
    }

    @PutMapping("/{subscriptionId}/auto-renew")
    @Operation(summary = "Enable or disable auto-renewal")
    public ApiResponse<SubscriptionResponse> autoRenew(
            @PathVariable UUID subscriptionId, @Valid @RequestBody UpdateAutoRenewRequest request) {
        return ApiResponse.ok(
                subscriptionService.setAutoRenew(currentUserAccessor.requireUser(), subscriptionId, request), "Auto-renew updated");
    }
}
