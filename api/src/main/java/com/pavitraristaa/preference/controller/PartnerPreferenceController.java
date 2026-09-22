package com.pavitraristaa.preference.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.preference.dto.PartnerPreferenceRequest;
import com.pavitraristaa.preference.dto.PartnerPreferenceResponse;
import com.pavitraristaa.preference.service.PartnerPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/preferences")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Partner Preferences")
public class PartnerPreferenceController {

    private final PartnerPreferenceService partnerPreferenceService;
    private final CurrentUserAccessor currentUserAccessor;

    public PartnerPreferenceController(
            PartnerPreferenceService partnerPreferenceService,
            CurrentUserAccessor currentUserAccessor
    ) {
        this.partnerPreferenceService = partnerPreferenceService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping
    @Operation(summary = "Get partner preferences")
    public ApiResponse<PartnerPreferenceResponse> get() {
        return ApiResponse.ok(partnerPreferenceService.getMine(currentUserAccessor.requireUser()), "Partner preferences");
    }

    @PutMapping
    @Operation(summary = "Replace partner preferences")
    public ApiResponse<PartnerPreferenceResponse> replace(@Valid @RequestBody PartnerPreferenceRequest request) {
        return ApiResponse.ok(
                partnerPreferenceService.replaceMine(currentUserAccessor.requireUser(), request),
                "Partner preferences updated"
        );
    }
}
