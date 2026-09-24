package com.pavitraristaa.admin.controller;

import com.pavitraristaa.admin.dto.AdminSupportTicketResponse;
import com.pavitraristaa.admin.dto.AssignTicketRequest;
import com.pavitraristaa.admin.dto.ResolveTicketRequest;
import com.pavitraristaa.admin.service.AdminSupportService;
import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.api.PagedData;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.common.security.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/admin/support/tickets")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Support")
@PreAuthorize(Roles.MODERATOR_OR_ABOVE)
public class AdminSupportController {

    private final AdminSupportService adminSupportService;
    private final CurrentUserAccessor currentUserAccessor;

    public AdminSupportController(AdminSupportService adminSupportService, CurrentUserAccessor currentUserAccessor) {
        this.adminSupportService = adminSupportService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping
    @Operation(summary = "List support tickets (every user's)")
    public ApiResponse<PagedData<AdminSupportTicketResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(adminSupportService.list(status, page, size), "Support tickets");
    }

    @PostMapping("/{ticketId}/assign")
    @Operation(summary = "Assign a ticket to a moderator/admin (defaults to the caller)")
    public ApiResponse<AdminSupportTicketResponse> assign(
            @PathVariable UUID ticketId, @RequestBody(required = false) AssignTicketRequest request) {
        return ApiResponse.ok(adminSupportService.assign(currentUserAccessor.requireUser(), ticketId, request), "Ticket assigned");
    }

    @PostMapping("/{ticketId}/resolve")
    @Operation(summary = "Resolve a support ticket")
    public ApiResponse<AdminSupportTicketResponse> resolve(
            @PathVariable UUID ticketId, @RequestBody(required = false) ResolveTicketRequest request) {
        return ApiResponse.ok(adminSupportService.resolve(currentUserAccessor.requireUser(), ticketId, request), "Ticket resolved");
    }
}
