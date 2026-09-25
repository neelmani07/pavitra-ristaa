package com.pavitraristaa.subscriptions.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.subscriptions.entity.Payment;
import java.math.BigDecimal;

/**
 * The seam where a real payment provider (Razorpay, Stripe, ...) plugs in later - deliberately deferred, same
 * as OtpSender/LoggingOtpSender for SMS delivery. Everything around this interface (subscription lifecycle,
 * payment records, invoices, coupons) is fully built and testable today against the stub implementation
 * (AutoApprovePaymentGateway); swapping in a real provider means implementing this interface for real and
 * replacing the @Component wiring - no other class in this module needs to change.
 */
public interface PaymentGateway {

    GatewayResult charge(UserAccount user, BigDecimal amount, String currencyCode, String description);

    GatewayResult retry(Payment payment);

    record GatewayResult(boolean success, String providerPaymentId, String failureReason) {
    }
}
