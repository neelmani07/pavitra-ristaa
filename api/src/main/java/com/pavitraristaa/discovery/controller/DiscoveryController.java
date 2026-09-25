package com.pavitraristaa.discovery.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.discovery.dto.CollectionDetailResponse;
import com.pavitraristaa.discovery.dto.CollectionSummaryResponse;
import com.pavitraristaa.discovery.dto.CreateSavedSearchRequest;
import com.pavitraristaa.discovery.dto.DiscoverySearchRequest;
import com.pavitraristaa.discovery.dto.HomeResponse;
import com.pavitraristaa.discovery.dto.SavedSearchResponse;
import com.pavitraristaa.discovery.dto.SearchHistoryResponse;
import com.pavitraristaa.discovery.dto.UpdateSavedSearchRequest;
import com.pavitraristaa.discovery.service.DiscoveryCollectionService;
import com.pavitraristaa.discovery.service.DiscoveryService;
import com.pavitraristaa.discovery.service.RecommendationService;
import com.pavitraristaa.discovery.service.SavedSearchService;
import com.pavitraristaa.discovery.service.SearchHistoryService;
import com.pavitraristaa.profile.dto.UserSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    private final RecommendationService recommendationService;
    private final SavedSearchService savedSearchService;
    private final SearchHistoryService searchHistoryService;
    private final DiscoveryCollectionService discoveryCollectionService;
    private final CurrentUserAccessor currentUserAccessor;

    public DiscoveryController(
            DiscoveryService discoveryService,
            RecommendationService recommendationService,
            SavedSearchService savedSearchService,
            SearchHistoryService searchHistoryService,
            DiscoveryCollectionService discoveryCollectionService,
            CurrentUserAccessor currentUserAccessor
    ) {
        this.discoveryService = discoveryService;
        this.recommendationService = recommendationService;
        this.savedSearchService = savedSearchService;
        this.searchHistoryService = searchHistoryService;
        this.discoveryCollectionService = discoveryCollectionService;
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

    @GetMapping("/discovery/recommendations")
    @Operation(summary = "Get persisted recommendations")
    public ApiResponse<List<UserSummaryResponse>> recommendations(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.ok(
                recommendationService.list(currentUserAccessor.requireUser(), page, size), "Recommendations");
    }

    @PostMapping("/discovery/recommendations/refresh")
    @Operation(summary = "Refresh recommendation set")
    public ApiResponse<Void> refreshRecommendations() {
        int count = recommendationService.refresh(currentUserAccessor.requireUser());
        return ApiResponse.ok("Generated " + count + " recommendations");
    }

    @GetMapping("/discovery/saved-searches")
    @Operation(summary = "List saved searches")
    public ApiResponse<List<SavedSearchResponse>> listSavedSearches() {
        return ApiResponse.ok(savedSearchService.list(currentUserAccessor.requireUser()), "Saved searches");
    }

    @PostMapping("/discovery/saved-searches")
    @Operation(summary = "Create saved search")
    public ApiResponse<SavedSearchResponse> createSavedSearch(@Valid @RequestBody CreateSavedSearchRequest request) {
        return ApiResponse.ok(savedSearchService.create(currentUserAccessor.requireUser(), request), "Saved search created");
    }

    @PutMapping("/discovery/saved-searches/{searchId}")
    @Operation(summary = "Update saved search")
    public ApiResponse<SavedSearchResponse> updateSavedSearch(
            @PathVariable Long searchId, @RequestBody UpdateSavedSearchRequest request) {
        return ApiResponse.ok(
                savedSearchService.update(currentUserAccessor.requireUser(), searchId, request), "Saved search updated");
    }

    @DeleteMapping("/discovery/saved-searches/{searchId}")
    @Operation(summary = "Delete saved search")
    public ApiResponse<Void> deleteSavedSearch(@PathVariable Long searchId) {
        savedSearchService.delete(currentUserAccessor.requireUser(), searchId);
        return ApiResponse.ok("Saved search deleted");
    }

    @GetMapping("/discovery/search-history")
    @Operation(summary = "List recent search history")
    public ApiResponse<List<SearchHistoryResponse>> searchHistory(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.ok(
                searchHistoryService.list(currentUserAccessor.requireUser(), page, size), "Search history");
    }

    @DeleteMapping("/discovery/search-history")
    @Operation(summary = "Clear search history")
    public ApiResponse<Void> clearSearchHistory() {
        searchHistoryService.clear(currentUserAccessor.requireUser());
        return ApiResponse.ok("Search history cleared");
    }

    @GetMapping("/discovery/collections")
    @Operation(summary = "List discovery collections")
    public ApiResponse<List<CollectionSummaryResponse>> listCollections() {
        return ApiResponse.ok(discoveryCollectionService.list(), "Collections");
    }

    @GetMapping("/discovery/collections/{collectionId}")
    @Operation(summary = "Get collection details and members")
    public ApiResponse<CollectionDetailResponse> getCollection(
            @PathVariable String collectionId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.ok(
                discoveryCollectionService.getOne(currentUserAccessor.requireUser(), collectionId, page, size), "Collection");
    }
}
