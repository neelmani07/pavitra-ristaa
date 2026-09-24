package com.pavitraristaa.admin.controller;

import com.pavitraristaa.admin.dto.AuditLogResponse;
import com.pavitraristaa.admin.service.AuditLogService;
import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.api.PagedData;
import com.pavitraristaa.common.security.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only, and deliberately a level up from moderators - it's the record of what every admin/moderator did. */
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Audit Logs")
@PreAuthorize(Roles.ADMIN_OR_ABOVE)
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    public AdminAuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @Operation(summary = "List audit log entries")
    public ApiResponse<PagedData<AuditLogResponse>> list(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(auditLogService.list(page, size), "Audit log");
    }
}
