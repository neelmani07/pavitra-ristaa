package com.pavitraristaa.media.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.media.dto.CompleteUploadRequest;
import com.pavitraristaa.media.dto.CreateUploadUrlRequest;
import com.pavitraristaa.media.dto.MediaFileResponse;
import com.pavitraristaa.media.dto.UploadUrlResponse;
import com.pavitraristaa.media.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/media")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Media")
public class MediaController {

    private final MediaService mediaService;
    private final CurrentUserAccessor currentUserAccessor;

    public MediaController(MediaService mediaService, CurrentUserAccessor currentUserAccessor) {
        this.mediaService = mediaService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @PostMapping("/upload-url")
    @Operation(summary = "Create a presigned upload URL")
    public ApiResponse<UploadUrlResponse> createUploadUrl(@Valid @RequestBody CreateUploadUrlRequest request) {
        return ApiResponse.ok(mediaService.createUploadUrl(currentUserAccessor.requireUser(), request), "Upload URL created");
    }

    @PostMapping("/complete")
    @Operation(summary = "Complete a media upload")
    public ApiResponse<MediaFileResponse> complete(@Valid @RequestBody CompleteUploadRequest request) {
        return ApiResponse.ok(mediaService.complete(currentUserAccessor.requireUser(), request), "Upload completed");
    }

    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Delete owned media")
    public ApiResponse<Void> delete(@PathVariable UUID mediaId) {
        mediaService.delete(currentUserAccessor.requireUser(), mediaId);
        return ApiResponse.ok("Media deleted");
    }
}
