package com.pavitraristaa.meta.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.meta.dto.OptionsResponse;
import com.pavitraristaa.meta.service.OptionsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meta")
@Tag(name = "Meta")
public class MetaController {

    private final OptionsService optionsService;

    public MetaController(OptionsService optionsService) {
        this.optionsService = optionsService;
    }

    @GetMapping("/options")
    @Operation(summary = "All dropdown options and fixed enum values in one call")
    public ResponseEntity<ApiResponse<OptionsResponse>> options() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(ApiResponse.ok(optionsService.options(), "Options"));
    }
}
