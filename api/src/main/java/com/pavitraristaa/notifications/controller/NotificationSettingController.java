package com.pavitraristaa.notifications.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.notifications.dto.NotificationSettingResponse;
import com.pavitraristaa.notifications.dto.UpdateNotificationSettingsRequest;
import com.pavitraristaa.notifications.service.NotificationSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notification-settings")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notifications")
public class NotificationSettingController {

    private final NotificationSettingService notificationSettingService;
    private final CurrentUserAccessor currentUserAccessor;

    public NotificationSettingController(NotificationSettingService notificationSettingService, CurrentUserAccessor currentUserAccessor) {
        this.notificationSettingService = notificationSettingService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping
    @Operation(summary = "Get notification preferences")
    public ApiResponse<List<NotificationSettingResponse>> get() {
        return ApiResponse.ok(notificationSettingService.get(currentUserAccessor.requireUser()), "Notification settings");
    }

    @PutMapping
    @Operation(summary = "Update notification preferences")
    public ApiResponse<List<NotificationSettingResponse>> update(@Valid @RequestBody UpdateNotificationSettingsRequest request) {
        return ApiResponse.ok(
                notificationSettingService.update(currentUserAccessor.requireUser(), request), "Notification settings updated");
    }
}
