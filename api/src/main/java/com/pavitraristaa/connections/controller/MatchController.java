package com.pavitraristaa.connections.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.connections.dto.CompatibilityResponse;
import com.pavitraristaa.connections.dto.MatchResponse;
import com.pavitraristaa.connections.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matches")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Connections")
public class MatchController {

    private final MatchService matchService;
    private final CurrentUserAccessor currentUserAccessor;

    public MatchController(MatchService matchService, CurrentUserAccessor currentUserAccessor) {
        this.matchService = matchService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping
    @Operation(summary = "List active matches")
    public ApiResponse<List<MatchResponse>> list(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(matchService.listActive(currentUserAccessor.requireUser(), page, size), "Matches");
    }

    @GetMapping("/history")
    @Operation(summary = "List match history")
    public ApiResponse<List<MatchResponse>> history(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(matchService.listHistory(currentUserAccessor.requireUser(), page, size), "Match history");
    }

    @GetMapping("/{matchId}")
    @Operation(summary = "Get match details")
    public ApiResponse<MatchResponse> getOne(@PathVariable UUID matchId) {
        return ApiResponse.ok(matchService.getOne(currentUserAccessor.requireUser(), matchId), "Match");
    }

    @PostMapping("/{matchId}/unmatch")
    @Operation(summary = "End a match")
    public ApiResponse<MatchResponse> unmatch(@PathVariable UUID matchId) {
        return ApiResponse.ok(matchService.unmatch(currentUserAccessor.requireUser(), matchId), "Match ended");
    }

    @GetMapping("/{matchId}/compatibility")
    @Operation(summary = "Get compatibility information")
    public ApiResponse<CompatibilityResponse> compatibility(@PathVariable UUID matchId) {
        return ApiResponse.ok(matchService.compatibility(currentUserAccessor.requireUser(), matchId), "Compatibility");
    }
}
