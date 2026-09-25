package com.pavitraristaa.admin.controller;

import com.pavitraristaa.admin.dto.AdminAppealResponse;
import com.pavitraristaa.admin.dto.ResolveAppealRequest;
import com.pavitraristaa.admin.service.AdminAppealService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/appeals")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Appeals")
@PreAuthorize(Roles.MODERATOR_OR_ABOVE)
public class AdminAppealController {

    private final AdminAppealService adminAppealService;
    private final CurrentUserAccessor currentUserAccessor;

    public AdminAppealController(AdminAppealService adminAppealService, CurrentUserAccessor currentUserAccessor) {
        this.adminAppealService = adminAppealService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping
    @Operation(summary = "List appeals")
    public ApiResponse<PagedData<AdminAppealResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(adminAppealService.list(status, page, size), "Appeals");
    }

    @GetMapping("/{appealId}")
    @Operation(summary = "Get an appeal")
    public ApiResponse<AdminAppealResponse> getOne(@PathVariable UUID appealId) {
        return ApiResponse.ok(adminAppealService.getOne(appealId), "Appeal");
    }

    @PostMapping("/{appealId}/resolve")
    @Operation(summary = "Approve or reject an appeal")
    public ApiResponse<AdminAppealResponse> resolve(@PathVariable UUID appealId, @Valid @RequestBody ResolveAppealRequest request) {
        return ApiResponse.ok(adminAppealService.resolve(currentUserAccessor.requireUser(), appealId, request), "Appeal resolved");
    }
}
