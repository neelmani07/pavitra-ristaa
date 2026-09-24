package com.pavitraristaa.admin.event;

import com.pavitraristaa.auth.entity.UserAccount;

/** Published when an admin approves or rejects a pending profile verification. */
public record VerificationDecidedEvent(UserAccount targetUser, boolean approved) {
}
