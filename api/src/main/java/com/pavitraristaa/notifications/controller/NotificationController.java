package com.pavitraristaa.notifications.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.notifications.dto.NotificationResponse;
import com.pavitraristaa.notifications.service.NotificationService;
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
@RequestMapping("/api/v1/notifications")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserAccessor currentUserAccessor;

    public NotificationController(NotificationService notificationService, CurrentUserAccessor currentUserAccessor) {
        this.notificationService = notificationService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping
    @Operation(summary = "List my notifications")
    public ApiResponse<List<NotificationResponse>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(
                notificationService.list(currentUserAccessor.requireUser(), type, unreadOnly, page, size), "Notifications");
    }

    @GetMapping("/{notificationId}")
    @Operation(summary = "Get notification details")
    public ApiResponse<NotificationResponse> getOne(@PathVariable UUID notificationId) {
        return ApiResponse.ok(notificationService.getOne(currentUserAccessor.requireUser(), notificationId), "Notification");
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Delete notification")
    public ApiResponse<Void> delete(@PathVariable UUID notificationId) {
        notificationService.delete(currentUserAccessor.requireUser(), notificationId);
        return ApiResponse.ok("Notification deleted");
    }

    @PostMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable UUID notificationId) {
        return ApiResponse.ok(notificationService.markRead(currentUserAccessor.requireUser(), notificationId), "Notification marked read");
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllRead(currentUserAccessor.requireUser());
        return ApiResponse.ok("All notifications marked read");
    }
}
