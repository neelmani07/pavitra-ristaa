package com.pavitraristaa.subscriptions.event;

import com.pavitraristaa.auth.entity.UserAccount;

/** Published whenever a charge or a retry fails. */
public record PaymentFailedEvent(UserAccount user, String reason) {
}
