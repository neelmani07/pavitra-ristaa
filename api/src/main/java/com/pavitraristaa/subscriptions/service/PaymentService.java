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
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /** Called by SubscriptionService.start() in the same transaction. Returns whether the charge succeeded. */
    @Transactional
    boolean chargeFor(UserAccount user, Subscription subscription, BigDecimal amount, String currencyCode, String description) {
        Payment payment = new Payment();
        payment.setUuid(UUID.randomUUID());
        payment.setUser(user);
        payment.setSubscription(subscription);
        payment.setProvider(AutoApprovePaymentGateway.PROVIDER);
        payment.setAmount(amount);
        payment.setCurrencyCode(currencyCode);
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setCreatedAt(Instant.now());
        Payment saved = paymentRepository.save(payment);

        PaymentGateway.GatewayResult result = paymentGateway.charge(user, amount, currencyCode, description);
        return applyResult(saved, result);
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

    @Transactional
    public PaymentResponse retry(AuthenticatedUser principal, UUID paymentId) {
        UserAccount self = authService.requireUsable(principal);
        Payment payment = requireOwned(self, paymentId);
        if (payment.getStatus() != PaymentStatus.FAILED) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Only a failed payment can be retried");
        }
        PaymentGateway.GatewayResult result = paymentGateway.retry(payment);
        applyResult(payment, result);
        return toResponse(payment);
    }

    /** Persists the gateway's outcome, issues an invoice and activates a PENDING subscription on success. */
    private boolean applyResult(Payment payment, PaymentGateway.GatewayResult result) {
        payment.setProviderPaymentId(result.providerPaymentId());
        if (result.success()) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(Instant.now());
            paymentRepository.save(payment);
            invoiceService.issueForPayment(payment);
            activatePendingSubscription(payment.getSubscription());
            eventPublisher.publishEvent(new PaymentSucceededEvent(payment.getUser(), payment.getAmount(), payment.getCurrencyCode()));
            return true;
        }
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        eventPublisher.publishEvent(new PaymentFailedEvent(payment.getUser(), result.failureReason()));
        return false;
    }

    private void activatePendingSubscription(Subscription subscription) {
        if (subscription != null && subscription.getStatus() == SubscriptionStatus.PENDING) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setUpdatedAt(Instant.now());
            subscriptionRepository.save(subscription);
        }
    }

    private Payment requireOwned(UserAccount self, UUID paymentId) {
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
