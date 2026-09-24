package com.pavitraristaa.support.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.support.dto.HelpResponse;
import com.pavitraristaa.support.dto.LegalDocumentResponse;
import com.pavitraristaa.support.service.HelpContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Support")
public class HelpController {

    private final HelpContentService helpContentService;

    public HelpController(HelpContentService helpContentService) {
        this.helpContentService = helpContentService;
    }

    @GetMapping("/help")
    @Operation(summary = "Get help center content")
    public ApiResponse<HelpResponse> help() {
        return ApiResponse.ok(helpContentService.help(), "Help center");
    }

    @GetMapping("/legal/{documentType}")
    @Operation(summary = "Get a legal document")
    public ApiResponse<LegalDocumentResponse> legal(@PathVariable String documentType) {
        return ApiResponse.ok(helpContentService.legalDocument(documentType), "Legal document");
    }
}
