package com.pavitraristaa.admin.controller;

import com.pavitraristaa.admin.dto.AdminReportResponse;
import com.pavitraristaa.admin.dto.ResolveReportRequest;
import com.pavitraristaa.admin.service.AdminReportService;
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
@RequestMapping("/api/v1/admin/reports")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Reports")
@PreAuthorize(Roles.MODERATOR_OR_ABOVE)
public class AdminReportController {

    private final AdminReportService adminReportService;
    private final CurrentUserAccessor currentUserAccessor;

    public AdminReportController(AdminReportService adminReportService, CurrentUserAccessor currentUserAccessor) {
        this.adminReportService = adminReportService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping
    @Operation(summary = "List reports")
    public ApiResponse<PagedData<AdminReportResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(adminReportService.list(status, page, size), "Reports");
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "Get a report")
    public ApiResponse<AdminReportResponse> getOne(@PathVariable UUID reportId) {
        return ApiResponse.ok(adminReportService.getOne(reportId), "Report");
    }

    @PostMapping("/{reportId}/resolve")
    @Operation(summary = "Resolve or dismiss a report, optionally opening a moderation action against the reported user")
    public ApiResponse<AdminReportResponse> resolve(@PathVariable UUID reportId, @Valid @RequestBody ResolveReportRequest request) {
        return ApiResponse.ok(adminReportService.resolve(currentUserAccessor.requireUser(), reportId, request), "Report resolved");
    }
}
