package com.pavitraristaa.admin.controller;

import com.pavitraristaa.admin.dto.AdminSettingResponse;
import com.pavitraristaa.admin.dto.UpdateSettingRequest;
import com.pavitraristaa.admin.service.AdminSettingService;
import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.common.security.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/settings")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Settings")
@PreAuthorize(Roles.MODERATOR_OR_ABOVE)
public class AdminSettingController {

    private final AdminSettingService adminSettingService;
    private final CurrentUserAccessor currentUserAccessor;

    public AdminSettingController(AdminSettingService adminSettingService, CurrentUserAccessor currentUserAccessor) {
        this.adminSettingService = adminSettingService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping
    @Operation(summary = "List all system settings")
    public ApiResponse<List<AdminSettingResponse>> list() {
        return ApiResponse.ok(adminSettingService.list(), "Settings");
    }

    @PutMapping("/{key}")
    @PreAuthorize(Roles.ADMIN_OR_ABOVE)
    @Operation(summary = "Create or update a system setting's value")
    public ApiResponse<AdminSettingResponse> upsert(@PathVariable String key, @Valid @RequestBody UpdateSettingRequest request) {
        return ApiResponse.ok(adminSettingService.upsert(currentUserAccessor.requireUser(), key, request), "Setting updated");
    }
}
