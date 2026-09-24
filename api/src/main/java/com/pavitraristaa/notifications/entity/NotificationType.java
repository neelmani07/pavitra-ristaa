package com.pavitraristaa.notifications.entity;

/**
 * The contract leaves notification.type / notification_setting.notification_type as free-form strings (no CHECK
 * constraint), but every producer in this codebase goes through this enum for compile-time safety. Chosen to
 * cover the categories the docs call out (relationship, profile, community/security, platform) with the events
 * this pass actually wires up. Deliberately NOT included: a per-chat-message type (MESSAGE_RECEIVED) - one
 * notification row per message would flood GET /notifications; real chat unread state is already served by
 * messaging's own read receipts / conversation unread counts, and push-only delivery for messages is a more
 * typical product choice anyway.
 */
public enum NotificationType {
    INTEREST_RECEIVED,
    MATCH_CREATED,
    VERIFICATION_APPROVED,
    VERIFICATION_REJECTED,
    REPORT_RESOLVED,
    ACCOUNT_SUSPENDED,
    ACCOUNT_REACTIVATED,
    SUPPORT_TICKET_RESOLVED
}
