package com.pavitraristaa.subscriptions.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.util.PaginationSupport;
import com.pavitraristaa.subscriptions.dto.PaymentResponse;
import com.pavitraristaa.subscriptions.entity.Payment;
import com.pavitraristaa.subscriptions.entity.PaymentStatus;
import com.pavitraristaa.subscriptions.entity.Subscription;
import com.pavitraristaa.subscriptions.entity.SubscriptionStatus;
import com.pavitraristaa.subscriptions.event.PaymentFailedEvent;
import com.pavitraristaa.subscriptions.event.PaymentSucceededEvent;
import com.pavitraristaa.subscriptions.repository.PaymentRepository;
import com.pavitraristaa.subscriptions.repository.SubscriptionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The record-keeping side of a charge: turning a confirmed or failed provider event into Payment/Invoice rows
 * and subscription state changes. Does not itself talk to the gateway or decide when a charge happened - that
 * lives in SubscriptionService (first checkout, retry) and RazorpayWebhookController (every later event).
 */
@Service
public class PaymentService {

    private final AuthService authService;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentGateway paymentGateway;
    private final InvoiceService invoiceService;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentService(
            AuthService authService,
            PaymentRepository paymentRepository,
            SubscriptionRepository subscriptionRepository,
            PaymentGateway paymentGateway,
            InvoiceService invoiceService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.authService = authService;
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentGateway = paymentGateway;
        this.invoiceService = invoiceService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Records a successful charge (the subscription's first, or a later renewal), issues an invoice, and
     * activates a PENDING subscription or extends an ACTIVE one's endsAt. Idempotent on providerPaymentId: a
     * webhook Razorpay redelivers, or one that arrives after the client-side confirm already processed the
     * same charge, is a no-op the second time - see the payment table's own UNIQUE(provider, provider_payment_id).
     */
    @Transactional
    public void confirmCharge(Subscription subscription, String providerPaymentId, BigDecimal amount, String currencyCode) {
        if (providerPaymentId != null
                && paymentRepository.existsByProviderAndProviderPaymentId(paymentGateway.providerName(), providerPaymentId)) {
            return;
        }
        Instant now = Instant.now();
        Payment payment = new Payment();
        payment.setUuid(UUID.randomUUID());
        payment.setUser(subscription.getUser());
        payment.setSubscription(subscription);
        payment.setProvider(paymentGateway.providerName());
        payment.setProviderPaymentId(providerPaymentId);
        payment.setAmount(amount);
        payment.setCurrencyCode(currencyCode);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(now);
        payment.setCreatedAt(now);
        Payment saved = paymentRepository.save(payment);
        invoiceService.issueForPayment(saved);
        activateOrExtend(subscription, now);
        eventPublisher.publishEvent(new PaymentSucceededEvent(subscription.getUser(), amount, currencyCode));
    }

    /** Records a failed charge attempt (first payment or a renewal) without changing the subscription's status
     *  itself - Razorpay drives its own renewal retry schedule; a failed first payment is retried through
     *  SubscriptionService.retryFailedCheckout(). */
    @Transactional
    public void recordFailedCharge(Subscription subscription, String failureReason) {
        Payment payment = new Payment();
        payment.setUuid(UUID.randomUUID());
        payment.setUser(subscription.getUser());
        payment.setSubscription(subscription);
        payment.setProvider(paymentGateway.providerName());
        payment.setAmount(subscription.getPlan().getPrice());
        payment.setCurrencyCode(subscription.getPlan().getCurrencyCode());
        payment.setStatus(PaymentStatus.FAILED);
        payment.setCreatedAt(Instant.now());
        paymentRepository.save(payment);
        eventPublisher.publishEvent(new PaymentFailedEvent(subscription.getUser(), failureReason));
    }

    private void activateOrExtend(Subscription subscription, Instant now) {
        var plan = subscription.getPlan();
        if (subscription.getStatus() == SubscriptionStatus.PENDING) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setEndsAt(now.plus(plan.getDurationDays(), ChronoUnit.DAYS));
        } else if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            // A renewal charge - extend from whichever is later: the current endsAt, or now (covers a renewal
            // webhook that arrives after the previous cycle already lapsed).
            Instant base = subscription.getEndsAt() != null && subscription.getEndsAt().isAfter(now) ? subscription.getEndsAt() : now;
            subscription.setEndsAt(base.plus(plan.getDurationDays(), ChronoUnit.DAYS));
        }
        subscription.setUpdatedAt(now);
        subscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listMine(AuthenticatedUser principal, Integer page, Integer size) {
        UserAccount self = authService.requireUsable(principal);
        return paymentRepository.findByUserOrderByCreatedAtDesc(self, PaginationSupport.pageable(page, size))
                .map(this::toResponse)
                .getContent();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getOne(AuthenticatedUser principal, UUID paymentId) {
        UserAccount self = authService.requireUsable(principal);
        return toResponse(requireOwned(self, paymentId));
    }

    Payment requireOwned(UserAccount self, UUID paymentId) {
        Payment payment = paymentRepository.findByUuid(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND, "Payment not found"));
        if (!payment.getUser().getId().equals(self.getId())) {
            throw new ApiException(ErrorCode.PAYMENT_NOT_FOUND, "Payment not found");
        }
        return payment;
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getUuid(),
                payment.getSubscription() == null ? null : payment.getSubscription().getUuid(),
                payment.getProvider(), payment.getAmount(), payment.getCurrencyCode(), payment.getStatus().name(),
                payment.getPaymentMethod(), payment.getPaidAt());
    }
}
