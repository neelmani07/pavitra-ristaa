package com.pavitraristaa.subscriptions.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.subscriptions.entity.Plan;

/**
 * The seam where a real recurring-billing provider (Razorpay Subscriptions today) plugs in - deliberately kept
 * separate from the rest of the module the same way OtpSender/LoggingOtpSender separates SMS delivery. A real
 * provider is a mandate-based, asynchronous flow, not a synchronous "charge and get yes/no": creating a
 * subscription here only sets up the recurring authorization (UPI Autopay / card e-mandate); the actual money
 * movement is confirmed later, out of band, via a webhook (see RazorpayWebhookController) - or, for the stub
 * implementation used in dev/test, immediately and synchronously (SubscriptionCheckout.autoConfirmed()).
 */
public interface PaymentGateway {

    /** Recorded in payment.provider - "STUB" or "RAZORPAY", not a config value, so it's always accurate for
     *  whichever gateway actually processed a given payment. */
    String providerName();

    /** The public key/identifier a client-side checkout SDK needs, e.g. Razorpay's key_id. Constant for the
     *  life of the gateway (unlike the per-subscription id in SubscriptionCheckout), so callers can rebuild a
     *  SubscriptionResponse's checkout fields without holding on to the original SubscriptionCheckout. */
    String checkoutKeyId();

    /**
     * Sets up the recurring mandate for one billing cycle of `plan` and returns what the client needs to open
     * the provider's checkout UI to authorize it. No money has moved yet when this returns.
     */
    SubscriptionCheckout createSubscriptionCheckout(UserAccount user, Plan plan);

    /** Cancels the mandate at the provider so no further renewal charges occur - our own DB row alone
     *  cancelling is not enough. No-op-safe to call with an id the provider doesn't recognize. */
    void cancelSubscription(String providerSubscriptionId);

    /**
     * Whether this provider can discount an individual subscription's recurring charge amount. Razorpay Plans
     * fix the amount per plan; per-subscription discounting needs a Razorpay Dashboard-configured "Offer",
     * which is out of scope for this pass - see CouponService and SubscriptionService.start(). The stub
     * (AutoApprovePaymentGateway) returns true so the existing coupon-discount tests keep exercising real logic.
     */
    boolean supportsPerSubscriptionDiscount();

    record SubscriptionCheckout(String providerSubscriptionId, boolean autoConfirmed) {
    }
}
