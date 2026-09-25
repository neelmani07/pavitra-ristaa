package com.pavitraristaa.subscriptions.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.subscriptions.dto.StartSubscriptionRequest;
import com.pavitraristaa.subscriptions.dto.SubscriptionResponse;
import com.pavitraristaa.subscriptions.dto.UpdateAutoRenewRequest;
import com.pavitraristaa.subscriptions.entity.Payment;
import com.pavitraristaa.subscriptions.entity.PaymentStatus;
import com.pavitraristaa.subscriptions.entity.Plan;
import com.pavitraristaa.subscriptions.entity.Subscription;
import com.pavitraristaa.subscriptions.entity.SubscriptionStatus;
import com.pavitraristaa.subscriptions.repository.SubscriptionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the subscription lifecycle and its side of the checkout flow (creating and retrying the provider's
 * recurring mandate). The record-keeping half of a confirmed/failed charge - Payment/Invoice rows, activating
 * or extending the subscription - lives in PaymentService, called from here on the stub's auto-confirm path
 * and from RazorpayWebhookController on the real one; see PaymentService.confirmCharge().
 */
@Service
public class SubscriptionService {

    private static final Set<SubscriptionStatus> BLOCKS_NEW_CHECKOUT = EnumSet.of(SubscriptionStatus.PENDING, SubscriptionStatus.ACTIVE);

    private final AuthService authService;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanService planService;
    private final CouponService couponService;
    private final PaymentGateway paymentGateway;
    private final PaymentService paymentService;

    public SubscriptionService(
            AuthService authService,
            SubscriptionRepository subscriptionRepository,
            PlanService planService,
            CouponService couponService,
            PaymentGateway paymentGateway,
            PaymentService paymentService
    ) {
        this.authService = authService;
        this.subscriptionRepository = subscriptionRepository;
        this.planService = planService;
        this.couponService = couponService;
        this.paymentGateway = paymentGateway;
        this.paymentService = paymentService;
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getCurrent(AuthenticatedUser principal) {
        UserAccount self = authService.requireUsable(principal);
        return subscriptionRepository.findFirstByUserOrderByCreatedAtDesc(self).map(this::toResponse).orElse(null);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getOne(AuthenticatedUser principal, UUID subscriptionId) {
        UserAccount self = authService.requireUsable(principal);
        return toResponse(requireOwned(self, subscriptionId));
    }

    @Transactional
    public SubscriptionResponse start(AuthenticatedUser principal, StartSubscriptionRequest request) {
        UserAccount self = authService.requireUsable(principal);
        if (subscriptionRepository.existsByUserAndStatusIn(self, BLOCKS_NEW_CHECKOUT)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "You already have an active or pending subscription");
        }
        Plan plan = planService.requirePlan(request.planCode());
        BigDecimal chargeAmount = plan.getPrice();
        if (request.couponCode() != null && !request.couponCode().isBlank()) {
            if (!paymentGateway.supportsPerSubscriptionDiscount()) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR,
                        "Coupons cannot be applied to recurring subscriptions with the current payment provider yet");
            }
            chargeAmount = couponService.requireAndRedeem(request.couponCode(), plan.getCode(), plan.getPrice());
        }

        Instant now = Instant.now();
        Subscription subscription = new Subscription();
        subscription.setUuid(UUID.randomUUID());
        subscription.setUser(self);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setStartsAt(now);
        subscription.setAutoRenew(false);
        subscription.setCreatedAt(now);
        subscription.setUpdatedAt(now);
        Subscription saved = subscriptionRepository.save(subscription);

        PaymentGateway.SubscriptionCheckout checkout = paymentGateway.createSubscriptionCheckout(self, plan);
        saved.setProviderSubscriptionId(checkout.providerSubscriptionId());
        subscriptionRepository.save(saved);
        if (checkout.autoConfirmed()) {
            paymentService.confirmCharge(saved, "STUB-PAY-" + UUID.randomUUID(), chargeAmount, plan.getCurrencyCode());
        }
        return toResponse(saved);
    }

    /**
     * The only retry path this API exposes: a subscription whose very first mandate authorization failed
     * (still PENDING). Once a subscription is ACTIVE, a failed renewal is retried by the provider itself on
     * its own schedule (Razorpay retries a failed UPI Autopay/e-mandate charge automatically) - there is
     * nothing for our API to trigger there.
     */
    @Transactional
    public SubscriptionResponse retryFailedCheckout(AuthenticatedUser principal, UUID paymentId) {
        UserAccount self = authService.requireUsable(principal);
        Payment payment = paymentService.requireOwned(self, paymentId);
        if (payment.getStatus() != PaymentStatus.FAILED) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Only a failed payment can be retried");
        }
        Subscription subscription = payment.getSubscription();
        if (subscription == null || subscription.getStatus() != SubscriptionStatus.PENDING) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "Only a failed first-payment attempt on a still-pending subscription can be retried");
        }
        PaymentGateway.SubscriptionCheckout checkout = paymentGateway.createSubscriptionCheckout(self, subscription.getPlan());
        subscription.setProviderSubscriptionId(checkout.providerSubscriptionId());
        subscriptionRepository.save(subscription);
        if (checkout.autoConfirmed()) {
            paymentService.confirmCharge(
                    subscription, "STUB-PAY-" + UUID.randomUUID(), subscription.getPlan().getPrice(), subscription.getPlan().getCurrencyCode());
        }
        return toResponse(subscription);
    }

    @Transactional
    public SubscriptionResponse cancel(AuthenticatedUser principal, UUID subscriptionId) {
        UserAccount self = authService.requireUsable(principal);
        Subscription subscription = requireOwned(self, subscriptionId);
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED || subscription.getStatus() == SubscriptionStatus.EXPIRED) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "This subscription is already " + subscription.getStatus());
        }
        if (subscription.getProviderSubscriptionId() != null) {
            paymentGateway.cancelSubscription(subscription.getProviderSubscriptionId());
        }
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setAutoRenew(false);
        subscription.setCancelledAt(Instant.now());
        subscription.setUpdatedAt(Instant.now());
        return toResponse(subscriptionRepository.save(subscription));
    }

    @Transactional
    public SubscriptionResponse setAutoRenew(AuthenticatedUser principal, UUID subscriptionId, UpdateAutoRenewRequest request) {
        UserAccount self = authService.requireUsable(principal);
        Subscription subscription = requireOwned(self, subscriptionId);
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new ApiException(ErrorCode.SUBSCRIPTION_NOT_ACTIVE, "Only an active subscription's auto-renew can be changed");
        }
        subscription.setAutoRenew(request.autoRenew());
        subscription.setUpdatedAt(Instant.now());
        return toResponse(subscriptionRepository.save(subscription));
    }

    private Subscription requireOwned(UserAccount self, UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findByUuid(subscriptionId)
                .orElseThrow(() -> new ApiException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "Subscription not found"));
        if (!subscription.getUser().getId().equals(self.getId())) {
            throw new ApiException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "Subscription not found");
        }
        return subscription;
    }

    /** checkoutSubscriptionId/checkoutKeyId are only meaningful while a mandate is still awaiting authorization. */
    private SubscriptionResponse toResponse(Subscription subscription) {
        boolean pending = subscription.getStatus() == SubscriptionStatus.PENDING && subscription.getProviderSubscriptionId() != null;
        return new SubscriptionResponse(
                subscription.getUuid(), planService.toResponse(subscription.getPlan()), subscription.getStatus().name(),
                subscription.getStartsAt(), subscription.getEndsAt(), subscription.isAutoRenew(),
                pending ? subscription.getProviderSubscriptionId() : null,
                pending ? paymentGateway.checkoutKeyId() : null);
    }
}
