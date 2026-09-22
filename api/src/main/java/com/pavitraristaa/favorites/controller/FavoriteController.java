package com.pavitraristaa.favorites.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.favorites.service.FavoriteService;
import com.pavitraristaa.profile.dto.UserSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/favorites")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final CurrentUserAccessor currentUserAccessor;

    public FavoriteController(FavoriteService favoriteService, CurrentUserAccessor currentUserAccessor) {
        this.favoriteService = favoriteService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping
    @Operation(summary = "List saved/favorited profiles")
    public ApiResponse<List<UserSummaryResponse>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.ok(favoriteService.listMine(currentUserAccessor.requireUser(), page, size), "Favorites");
    }

    @PostMapping("/{profileId}")
    @Operation(summary = "Add profile to favorites")
    public ApiResponse<Void> add(@PathVariable UUID profileId) {
        favoriteService.add(currentUserAccessor.requireUser(), profileId);
        return ApiResponse.ok("Added to favorites");
    }

    @DeleteMapping("/{profileId}")
    @Operation(summary = "Remove profile from favorites")
    public ApiResponse<Void> remove(@PathVariable UUID profileId) {
        favoriteService.remove(currentUserAccessor.requireUser(), profileId);
        return ApiResponse.ok("Removed from favorites");
    }
}
