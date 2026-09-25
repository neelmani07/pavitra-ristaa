package com.pavitraristaa.subscriptions.event;

import com.pavitraristaa.auth.entity.UserAccount;
import java.math.BigDecimal;

/** Published whenever a charge or a retry succeeds. */
public record PaymentSucceededEvent(UserAccount user, BigDecimal amount, String currencyCode) {
}
