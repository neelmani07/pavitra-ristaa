package com.pavitraristaa.trust.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.profile.dto.UserSummaryResponse;
import com.pavitraristaa.trust.service.BlockService;
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
@RequestMapping("/api/v1/blocks")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Trust & Safety")
public class BlockController {

    private final BlockService blockService;
    private final CurrentUserAccessor currentUserAccessor;

    public BlockController(BlockService blockService, CurrentUserAccessor currentUserAccessor) {
        this.blockService = blockService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping
    @Operation(summary = "List blocked users")
    public ApiResponse<List<UserSummaryResponse>> list(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(blockService.listMine(currentUserAccessor.requireUser(), page, size), "Blocked users");
    }

    @PostMapping("/{userId}")
    @Operation(summary = "Block a user")
    public ApiResponse<Void> block(@PathVariable UUID userId) {
        blockService.block(currentUserAccessor.requireUser(), userId);
        return ApiResponse.ok("User blocked");
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Unblock a user")
    public ApiResponse<Void> unblock(@PathVariable UUID userId) {
        blockService.unblock(currentUserAccessor.requireUser(), userId);
        return ApiResponse.ok("User unblocked");
    }
}
