package com.pavitraristaa.subscriptions.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.subscriptions.dto.StartSubscriptionRequest;
import com.pavitraristaa.subscriptions.dto.SubscriptionResponse;
import com.pavitraristaa.subscriptions.dto.UpdateAutoRenewRequest;
import com.pavitraristaa.subscriptions.entity.Plan;
import com.pavitraristaa.subscriptions.entity.Subscription;
import com.pavitraristaa.subscriptions.entity.SubscriptionStatus;
import com.pavitraristaa.subscriptions.repository.SubscriptionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the subscription lifecycle. Starting a subscription also drives the payment/invoice flow (via
 * PaymentService) in the same transaction, so a subscription is never left PENDING with no corresponding
 * payment attempt - see PaymentService.chargeFor().
 */
@Service
public class SubscriptionService {

    private static final Set<SubscriptionStatus> BLOCKS_NEW_CHECKOUT = EnumSet.of(SubscriptionStatus.PENDING, SubscriptionStatus.ACTIVE);

    private final AuthService authService;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanService planService;
    private final CouponService couponService;
    private final PaymentService paymentService;

    public SubscriptionService(
            AuthService authService,
            SubscriptionRepository subscriptionRepository,
            PlanService planService,
            CouponService couponService,
            PaymentService paymentService
    ) {
        this.authService = authService;
        this.subscriptionRepository = subscriptionRepository;
        this.planService = planService;
        this.couponService = couponService;
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
        BigDecimal amount = request.couponCode() == null || request.couponCode().isBlank()
                ? plan.getPrice()
                : couponService.requireAndRedeem(request.couponCode(), plan.getCode(), plan.getPrice());

        Instant now = Instant.now();
        Subscription subscription = new Subscription();
        subscription.setUuid(UUID.randomUUID());
        subscription.setUser(self);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setStartsAt(now);
        subscription.setEndsAt(now.plus(plan.getDurationDays(), ChronoUnit.DAYS));
        subscription.setAutoRenew(false);
        subscription.setCreatedAt(now);
        subscription.setUpdatedAt(now);
        Subscription saved = subscriptionRepository.save(subscription);

        // chargeFor() mutates `saved` to ACTIVE in place on success (same managed entity, same transaction) -
        // see PaymentService.activatePendingSubscription(). A failed charge leaves it PENDING, matching a real
        // gateway's "checkout started but payment didn't go through yet" state.
        paymentService.chargeFor(self, saved, amount, plan.getCurrencyCode(), "Subscription: " + plan.getName());
        return toResponse(saved);
    }

    @Transactional
    public SubscriptionResponse cancel(AuthenticatedUser principal, UUID subscriptionId) {
        UserAccount self = authService.requireUsable(principal);
        Subscription subscription = requireOwned(self, subscriptionId);
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED || subscription.getStatus() == SubscriptionStatus.EXPIRED) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "This subscription is already " + subscription.getStatus());
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

    private SubscriptionResponse toResponse(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getUuid(), planService.toResponse(subscription.getPlan()), subscription.getStatus().name(),
                subscription.getStartsAt(), subscription.getEndsAt(), subscription.isAutoRenew());
    }
}
