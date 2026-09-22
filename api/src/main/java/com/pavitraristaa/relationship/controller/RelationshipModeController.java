package com.pavitraristaa.relationship.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.relationship.dto.RelationshipModeResponse;
import com.pavitraristaa.relationship.dto.ReplaceRelationshipModesRequest;
import com.pavitraristaa.relationship.service.RelationshipModeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Relationship Modes")
public class RelationshipModeController {

    private final RelationshipModeService relationshipModeService;
    private final CurrentUserAccessor currentUserAccessor;

    public RelationshipModeController(
            RelationshipModeService relationshipModeService,
            CurrentUserAccessor currentUserAccessor
    ) {
        this.relationshipModeService = relationshipModeService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping("/relationship-modes")
    @Operation(summary = "List active relationship modes")
    public ApiResponse<List<RelationshipModeResponse>> listActive() {
        return ApiResponse.ok(relationshipModeService.listActive(), "Relationship modes");
    }

    @GetMapping("/me/relationship-modes")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get my relationship modes")
    public ApiResponse<List<RelationshipModeResponse>> listMine() {
        return ApiResponse.ok(relationshipModeService.listMine(currentUserAccessor.requireUser()), "My relationship modes");
    }

    @PutMapping("/me/relationship-modes")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Replace my relationship modes")
    public ApiResponse<List<RelationshipModeResponse>> replaceMine(@RequestBody ReplaceRelationshipModesRequest request) {
        return ApiResponse.ok(
                relationshipModeService.replaceMine(currentUserAccessor.requireUser(), request),
                "Relationship modes updated"
        );
    }
}
