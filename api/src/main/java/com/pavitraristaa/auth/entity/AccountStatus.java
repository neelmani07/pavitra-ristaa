package com.pavitraristaa.auth.entity;

public enum AccountStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    /** Self-service: the account holder turned their own account off. Reversible via POST /auth/reactivate. */
    DEACTIVATED,
    /** Admin action (moderation). Not self-reversible; requires an admin/moderator to lift it. */
    SUSPENDED,
    BLOCKED,
    DELETED
}
