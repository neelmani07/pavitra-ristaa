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

/**
 * GET/POST/DELETE /shortlist mirror GET/POST/DELETE /favorites exactly - same three endpoints, same
 * {profileId} shape - and the contract's own status line groups them as a single item ("favorites/shortlist
 * mapping"), not two. Read literally: /shortlist is a second name for the same favorite table, not a second
 * table this schema never defined. This controller is a thin alias over FavoriteService rather than a
 * reimplementation, so favoriting and shortlisting a profile are the same action from either path.
 */
@RestController
@RequestMapping("/api/v1/shortlist")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Favorites")
public class ShortlistController {

    private final FavoriteService favoriteService;
    private final CurrentUserAccessor currentUserAccessor;

    public ShortlistController(FavoriteService favoriteService, CurrentUserAccessor currentUserAccessor) {
        this.favoriteService = favoriteService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping
    @Operation(summary = "List shortlisted profiles")
    public ApiResponse<List<UserSummaryResponse>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.ok(favoriteService.listMine(currentUserAccessor.requireUser(), page, size), "Shortlist");
    }

    @PostMapping("/{profileId}")
    @Operation(summary = "Add profile to shortlist")
    public ApiResponse<Void> add(@PathVariable UUID profileId) {
        favoriteService.add(currentUserAccessor.requireUser(), profileId);
        return ApiResponse.ok("Added to shortlist");
    }

    @DeleteMapping("/{profileId}")
    @Operation(summary = "Remove profile from shortlist")
    public ApiResponse<Void> remove(@PathVariable UUID profileId) {
        favoriteService.remove(currentUserAccessor.requireUser(), profileId);
        return ApiResponse.ok("Removed from shortlist");
    }
}
