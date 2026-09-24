package com.pavitraristaa.admin.controller;

import com.pavitraristaa.admin.dto.AdminVerificationResponse;
import com.pavitraristaa.admin.dto.VerificationDecisionRequest;
import com.pavitraristaa.admin.service.AdminVerificationService;
import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.api.PagedData;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.common.security.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/verifications")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Verifications")
@PreAuthorize(Roles.MODERATOR_OR_ABOVE)
public class AdminVerificationController {

    private final AdminVerificationService adminVerificationService;
    private final CurrentUserAccessor currentUserAccessor;

    public AdminVerificationController(AdminVerificationService adminVerificationService, CurrentUserAccessor currentUserAccessor) {
        this.adminVerificationService = adminVerificationService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping
    @Operation(summary = "List pending profile verifications")
    public ApiResponse<PagedData<AdminVerificationResponse>> listPending(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(adminVerificationService.listPending(page, size), "Pending verifications");
    }

    @PostMapping("/{verificationId}/approve")
    @Operation(summary = "Approve a pending verification")
    public ApiResponse<AdminVerificationResponse> approve(
            @PathVariable Long verificationId, @RequestBody(required = false) VerificationDecisionRequest request) {
        return ApiResponse.ok(
                adminVerificationService.approve(currentUserAccessor.requireUser(), verificationId, request), "Verification approved");
    }

    @PostMapping("/{verificationId}/reject")
    @Operation(summary = "Reject a pending verification")
    public ApiResponse<AdminVerificationResponse> reject(
            @PathVariable Long verificationId, @RequestBody(required = false) VerificationDecisionRequest request) {
        return ApiResponse.ok(
                adminVerificationService.reject(currentUserAccessor.requireUser(), verificationId, request), "Verification rejected");
    }
}
