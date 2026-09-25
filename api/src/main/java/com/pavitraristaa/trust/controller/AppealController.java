package com.pavitraristaa.trust.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.trust.dto.AppealResponse;
import com.pavitraristaa.trust.dto.SubmitAppealRequest;
import com.pavitraristaa.trust.service.AppealService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appeals")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Trust & Safety")
public class AppealController {

    private final AppealService appealService;
    private final CurrentUserAccessor currentUserAccessor;

    public AppealController(AppealService appealService, CurrentUserAccessor currentUserAccessor) {
        this.appealService = appealService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit an appeal")
    public ApiResponse<AppealResponse> submit(@Valid @RequestBody SubmitAppealRequest request) {
        return ApiResponse.ok(appealService.submit(currentUserAccessor.requireUser(), request), "Appeal submitted");
    }
}
