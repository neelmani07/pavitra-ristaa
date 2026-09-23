package com.pavitraristaa.trust.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.trust.dto.CreateReportRequest;
import com.pavitraristaa.trust.dto.ReportReasonResponse;
import com.pavitraristaa.trust.dto.ReportResponse;
import com.pavitraristaa.trust.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Trust & Safety")
public class ReportController {

    private final ReportService reportService;
    private final CurrentUserAccessor currentUserAccessor;

    public ReportController(ReportService reportService, CurrentUserAccessor currentUserAccessor) {
        this.reportService = reportService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping("/reports/reasons")
    @Operation(summary = "List active report reasons")
    public ApiResponse<List<ReportReasonResponse>> reasons() {
        return ApiResponse.ok(reportService.listReasons(), "Report reasons");
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Report a user, profile or message")
    public ApiResponse<ReportResponse> create(@Valid @RequestBody CreateReportRequest request) {
        return ApiResponse.ok(reportService.create(currentUserAccessor.requireUser(), request), "Report submitted");
    }
}
