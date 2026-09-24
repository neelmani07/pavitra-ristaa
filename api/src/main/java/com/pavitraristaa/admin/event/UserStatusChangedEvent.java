package com.pavitraristaa.admin.event;

import com.pavitraristaa.auth.entity.UserAccount;

/** Published when an admin suspends or (re)activates a user's account. */
public record UserStatusChangedEvent(UserAccount targetUser, String newStatus) {
}
