package com.pavitraristaa.trust.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.trust.dto.SubmitVerificationRequest;
import com.pavitraristaa.trust.dto.VerificationStatusResponse;
import com.pavitraristaa.trust.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/verification")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Trust & Safety")
public class VerificationController {

    private final VerificationService verificationService;
    private final CurrentUserAccessor currentUserAccessor;

    public VerificationController(VerificationService verificationService, CurrentUserAccessor currentUserAccessor) {
        this.verificationService = verificationService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping("/status")
    @Operation(summary = "Get current verification status")
    public ApiResponse<VerificationStatusResponse> status() {
        return ApiResponse.ok(verificationService.myStatus(currentUserAccessor.requireUser()), "Verification status");
    }

    @PostMapping("/requests")
    @Operation(summary = "Submit verification request")
    public ApiResponse<VerificationStatusResponse> submit(@RequestBody SubmitVerificationRequest request) {
        return ApiResponse.ok(verificationService.submit(currentUserAccessor.requireUser(), request), "Verification requested");
    }

    @GetMapping("/{verificationId}")
    @Operation(summary = "Get verification status/details")
    public ApiResponse<VerificationStatusResponse> getOne(@PathVariable Long verificationId) {
        return ApiResponse.ok(verificationService.getOne(currentUserAccessor.requireUser(), verificationId), "Verification");
    }
}
