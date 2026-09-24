package com.pavitraristaa.notifications.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.util.PaginationSupport;
import com.pavitraristaa.notifications.dto.NotificationResponse;
import com.pavitraristaa.notifications.entity.Notification;
import com.pavitraristaa.notifications.entity.NotificationType;
import com.pavitraristaa.notifications.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final AuthService authService;
    private final NotificationRepository notificationRepository;

    public NotificationService(AuthService authService, NotificationRepository notificationRepository) {
        this.authService = authService;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(AuthenticatedUser principal, String type, Boolean unreadOnly, Integer page, Integer size) {
        UserAccount self = authService.requireUsable(principal);
        Pageable pageable = PaginationSupport.pageable(page, size);
        boolean onlyUnread = Boolean.TRUE.equals(unreadOnly);

        if (type != null && !type.isBlank()) {
            NotificationType parsed = parseType(type);
            return (onlyUnread
                    ? notificationRepository.findByUserAndTypeAndReadFalseOrderByCreatedAtDesc(self, parsed, pageable)
                    : notificationRepository.findByUserAndTypeOrderByCreatedAtDesc(self, parsed, pageable))
                    .map(this::toResponse).getContent();
        }
        return (onlyUnread
                ? notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(self, pageable)
                : notificationRepository.findByUserOrderByCreatedAtDesc(self, pageable))
                .map(this::toResponse).getContent();
    }

    @Transactional(readOnly = true)
    public NotificationResponse getOne(AuthenticatedUser principal, UUID notificationId) {
        UserAccount self = authService.requireUsable(principal);
        return toResponse(requireOwned(self, notificationId));
    }

    @Transactional
    public void delete(AuthenticatedUser principal, UUID notificationId) {
        UserAccount self = authService.requireUsable(principal);
        notificationRepository.delete(requireOwned(self, notificationId));
    }

    @Transactional
    public NotificationResponse markRead(AuthenticatedUser principal, UUID notificationId) {
        UserAccount self = authService.requireUsable(principal);
        Notification notification = requireOwned(self, notificationId);
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
        }
        return toResponse(notification);
    }

    @Transactional
    public void markAllRead(AuthenticatedUser principal) {
        UserAccount self = authService.requireUsable(principal);
        notificationRepository.markAllRead(self, Instant.now());
    }

    private Notification requireOwned(UserAccount self, UUID notificationId) {
        Notification notification = notificationRepository.findByUuid(notificationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOTIFICATION_NOT_FOUND, "Notification not found"));
        if (!notification.getUser().getId().equals(self.getId())) {
            throw new ApiException(ErrorCode.NOTIFICATION_NOT_FOUND, "Notification not found");
        }
        return notification;
    }

    private NotificationType parseType(String type) {
        try {
            return NotificationType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid notification type", Map.of("type", type));
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getUuid(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getBody(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}
