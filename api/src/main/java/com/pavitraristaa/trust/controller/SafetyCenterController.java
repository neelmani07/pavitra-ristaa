package com.pavitraristaa.trust.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.trust.dto.SafetyCenterResponse;
import com.pavitraristaa.trust.service.SafetyCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/safety-center")
@Tag(name = "Trust & Safety")
public class SafetyCenterController {

    private final SafetyCenterService safetyCenterService;

    public SafetyCenterController(SafetyCenterService safetyCenterService) {
        this.safetyCenterService = safetyCenterService;
    }

    @GetMapping
    @Operation(summary = "Get safety center content")
    public ApiResponse<SafetyCenterResponse> get() {
        return ApiResponse.ok(safetyCenterService.content(), "Safety center");
    }
}
