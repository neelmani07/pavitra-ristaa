package com.pavitraristaa.subscriptions.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.subscriptions.entity.Plan;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stand-in until a real provider is configured (see PaymentGatewayConfig): every checkout auto-confirms
 * immediately with a synthetic "STUB-SUB-..." id, so the rest of the subscription/payment/invoice flow can be
 * built, tested and demoed without a Razorpay account. No money moves, no external call is made. Registered as
 * a bean only by PaymentGatewayConfig, not @Component itself, so exactly one PaymentGateway bean ever exists.
 */
class AutoApprovePaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(AutoApprovePaymentGateway.class);

    @Override
    public String providerName() {
        return "STUB";
    }

    @Override
    public String checkoutKeyId() {
        return "stub_key_id";
    }

    @Override
    public SubscriptionCheckout createSubscriptionCheckout(UserAccount user, Plan plan) {
        log.info(
                "Stub payment gateway auto-confirming a subscription checkout for user {} on plan {} - no real "
                        + "payment provider is configured yet",
                user.getUuid(), plan.getCode());
        return new SubscriptionCheckout("STUB-SUB-" + UUID.randomUUID(), true);
    }

    @Override
    public void cancelSubscription(String providerSubscriptionId) {
        log.info("Stub payment gateway acknowledging cancellation of subscription {}", providerSubscriptionId);
    }

    @Override
    public boolean supportsPerSubscriptionDiscount() {
        return true;
    }
}
