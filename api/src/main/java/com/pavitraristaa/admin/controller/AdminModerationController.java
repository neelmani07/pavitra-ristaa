package com.pavitraristaa.admin.controller;

import com.pavitraristaa.admin.dto.AdminModerationResponse;
import com.pavitraristaa.admin.dto.CreateModerationRequest;
import com.pavitraristaa.admin.dto.ResolveModerationRequest;
import com.pavitraristaa.admin.service.AdminModerationService;
import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.api.PagedData;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.common.security.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/moderation")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Moderation")
@PreAuthorize(Roles.MODERATOR_OR_ABOVE)
public class AdminModerationController {

    private final AdminModerationService adminModerationService;
    private final CurrentUserAccessor currentUserAccessor;

    public AdminModerationController(AdminModerationService adminModerationService, CurrentUserAccessor currentUserAccessor) {
        this.adminModerationService = adminModerationService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping
    @Operation(summary = "List moderation actions")
    public ApiResponse<PagedData<AdminModerationResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(adminModerationService.list(status, page, size), "Moderation actions");
    }

    @GetMapping("/{moderationId}")
    @Operation(summary = "Get a moderation action")
    public ApiResponse<AdminModerationResponse> getOne(@PathVariable UUID moderationId) {
        return ApiResponse.ok(adminModerationService.getOne(moderationId), "Moderation action");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open a standalone moderation action against a user (not tied to a report)")
    public ApiResponse<AdminModerationResponse> create(@Valid @RequestBody CreateModerationRequest request) {
        return ApiResponse.ok(adminModerationService.create(currentUserAccessor.requireUser(), request), "Moderation action opened");
    }

    @PostMapping("/{moderationId}/resolve")
    @Operation(summary = "Resolve a moderation action")
    public ApiResponse<AdminModerationResponse> resolve(
            @PathVariable UUID moderationId, @RequestBody(required = false) ResolveModerationRequest request) {
        return ApiResponse.ok(
                adminModerationService.resolve(currentUserAccessor.requireUser(), moderationId, request), "Moderation action resolved");
    }
}
