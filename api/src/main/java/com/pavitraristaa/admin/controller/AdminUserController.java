package com.pavitraristaa.admin.controller;

import com.pavitraristaa.admin.dto.AdminUserResponse;
import com.pavitraristaa.admin.dto.LoginHistoryResponse;
import com.pavitraristaa.admin.dto.SuspendUserRequest;
import com.pavitraristaa.admin.dto.UpdateUserRolesRequest;
import com.pavitraristaa.admin.service.AdminUserService;
import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.api.PagedData;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.common.security.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Users")
@PreAuthorize(Roles.MODERATOR_OR_ABOVE)
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final CurrentUserAccessor currentUserAccessor;

    public AdminUserController(AdminUserService adminUserService, CurrentUserAccessor currentUserAccessor) {
        this.adminUserService = adminUserService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping
    @Operation(summary = "Search/list users")
    public ApiResponse<PagedData<AdminUserResponse>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(adminUserService.search(status, search, page, size), "Users");
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get a user")
    public ApiResponse<AdminUserResponse> getOne(@PathVariable UUID userId) {
        return ApiResponse.ok(adminUserService.getOne(userId), "User");
    }

    @PutMapping("/{userId}/roles")
    @PreAuthorize(Roles.ADMIN_OR_ABOVE)
    @Operation(summary = "Reassign a user's roles (replaces the full set)")
    public ApiResponse<AdminUserResponse> updateRoles(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRolesRequest request) {
        return ApiResponse.ok(
                adminUserService.updateRoles(currentUserAccessor.requireUser(), userId, request), "Roles updated");
    }

    @PostMapping("/{userId}/suspend")
    @Operation(summary = "Suspend a user (admin-only status; not self-reversible)")
    public ApiResponse<AdminUserResponse> suspend(@PathVariable UUID userId, @Valid @RequestBody SuspendUserRequest request) {
        return ApiResponse.ok(adminUserService.suspend(currentUserAccessor.requireUser(), userId, request), "User suspended");
    }

    @PostMapping("/{userId}/activate")
    @Operation(summary = "Reactivate a suspended or deactivated user")
    public ApiResponse<AdminUserResponse> activate(@PathVariable UUID userId) {
        return ApiResponse.ok(adminUserService.activate(currentUserAccessor.requireUser(), userId), "User activated");
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize(Roles.ADMIN_OR_ABOVE)
    @Operation(summary = "Delete a user (admin action; irreversible like self-service account deletion)")
    public ApiResponse<Void> delete(@PathVariable UUID userId) {
        adminUserService.delete(currentUserAccessor.requireUser(), userId);
        return ApiResponse.ok("User deleted");
    }

    @GetMapping("/{userId}/login-history")
    @Operation(summary = "A user's login history")
    public ApiResponse<PagedData<LoginHistoryResponse>> loginHistory(
            @PathVariable UUID userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(adminUserService.loginHistory(userId, page, size), "Login history");
    }
}
