package com.pavitraristaa.discovery.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.discovery.dto.DiscoverySearchRequest;
import com.pavitraristaa.discovery.dto.HomeResponse;
import com.pavitraristaa.discovery.service.DiscoveryService;
import com.pavitraristaa.profile.dto.UserSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Discovery")
public class DiscoveryController {

    private final DiscoveryService discoveryService;
    private final CurrentUserAccessor currentUserAccessor;

    public DiscoveryController(DiscoveryService discoveryService, CurrentUserAccessor currentUserAccessor) {
        this.discoveryService = discoveryService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping("/home")
    @Operation(summary = "Get personalized home discovery payload")
    public ApiResponse<HomeResponse> home() {
        return ApiResponse.ok(discoveryService.home(currentUserAccessor.requireUser()), "Home");
    }

    @GetMapping("/discovery/profiles")
    @Operation(summary = "Browse discoverable profiles")
    public ApiResponse<List<UserSummaryResponse>> browse(
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) Long countryId,
            @RequestParam(required = false) Long stateId,
            @RequestParam(required = false) Long cityId,
            @Parameter(description = "Accepted for contract compatibility; not applied - the schema has no "
                    + "geographic coordinates to compute a real distance from.")
            @RequestParam(required = false) Integer distanceKm,
            @RequestParam(required = false) String spiritualCommunity,
            @RequestParam(required = false) String spiritualInterest,
            @RequestParam(required = false) String practice,
            @RequestParam(required = false) String anySpiritualProfession,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        List<UserSummaryResponse> profiles = discoveryService.browse(
                currentUserAccessor.requireUser(), mode, minAge, maxAge, countryId, stateId, cityId,
                spiritualCommunity, spiritualInterest, practice, anySpiritualProfession, page, size);
        return ApiResponse.ok(profiles, "Profiles");
    }

    @PostMapping("/discovery/search")
    @Operation(summary = "Search profiles using filters")
    public ApiResponse<List<UserSummaryResponse>> search(@RequestBody DiscoverySearchRequest request) {
        return ApiResponse.ok(discoveryService.search(currentUserAccessor.requireUser(), request), "Search results");
    }

    @PostMapping("/discovery/profiles/{profileId}/view")
    @Operation(summary = "Explicitly record a profile view")
    public ApiResponse<Void> recordView(@PathVariable UUID profileId) {
        discoveryService.recordView(currentUserAccessor.requireUser(), profileId);
        return ApiResponse.ok("View recorded");
    }

    @GetMapping("/discovery/recently-viewed")
    @Operation(summary = "List recently viewed profiles")
    public ApiResponse<List<UserSummaryResponse>> recentlyViewed(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.ok(
                discoveryService.recentlyViewed(currentUserAccessor.requireUser(), page, size), "Recently viewed");
    }
}
