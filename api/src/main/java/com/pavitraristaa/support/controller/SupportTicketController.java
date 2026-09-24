package com.pavitraristaa.support.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.support.dto.CreateSupportTicketRequest;
import com.pavitraristaa.support.dto.SupportTicketResponse;
import com.pavitraristaa.support.dto.UpdateSupportTicketRequest;
import com.pavitraristaa.support.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/support/tickets")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Support")
public class SupportTicketController {

    private final SupportTicketService supportTicketService;
    private final CurrentUserAccessor currentUserAccessor;

    public SupportTicketController(SupportTicketService supportTicketService, CurrentUserAccessor currentUserAccessor) {
        this.supportTicketService = supportTicketService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping
    @Operation(summary = "List my support tickets")
    public ApiResponse<List<SupportTicketResponse>> list(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(supportTicketService.listMine(currentUserAccessor.requireUser(), page, size), "Support tickets");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create support ticket")
    public ApiResponse<SupportTicketResponse> create(@Valid @RequestBody CreateSupportTicketRequest request) {
        return ApiResponse.ok(supportTicketService.create(currentUserAccessor.requireUser(), request), "Support ticket created");
    }

    @GetMapping("/{ticketId}")
    @Operation(summary = "Get support ticket")
    public ApiResponse<SupportTicketResponse> getOne(@PathVariable UUID ticketId) {
        return ApiResponse.ok(supportTicketService.getOne(currentUserAccessor.requireUser(), ticketId), "Support ticket");
    }

    @PutMapping("/{ticketId}")
    @Operation(summary = "Update support ticket from user side")
    public ApiResponse<SupportTicketResponse> update(
            @PathVariable UUID ticketId, @RequestBody UpdateSupportTicketRequest request) {
        return ApiResponse.ok(
                supportTicketService.update(currentUserAccessor.requireUser(), ticketId, request), "Support ticket updated");
    }
}
