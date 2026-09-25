package com.pavitraristaa.subscriptions.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.subscriptions.dto.InvoiceResponse;
import com.pavitraristaa.subscriptions.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invoices")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Subscription & Payments")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final CurrentUserAccessor currentUserAccessor;

    public InvoiceController(InvoiceService invoiceService, CurrentUserAccessor currentUserAccessor) {
        this.invoiceService = invoiceService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping
    @Operation(summary = "List invoices")
    public ApiResponse<List<InvoiceResponse>> list(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(invoiceService.listMine(currentUserAccessor.requireUser(), page, size), "Invoices");
    }

    @GetMapping("/{invoiceId}")
    @Operation(summary = "Get invoice details")
    public ApiResponse<InvoiceResponse> getOne(@PathVariable UUID invoiceId) {
        return ApiResponse.ok(invoiceService.getOne(currentUserAccessor.requireUser(), invoiceId), "Invoice");
    }
}
