package com.pavitraristaa.subscriptions.service;

import com.pavitraristaa.config.PavitraProperties;
import com.pavitraristaa.subscriptions.entity.Subscription;
import com.pavitraristaa.subscriptions.entity.SubscriptionStatus;
import com.pavitraristaa.subscriptions.repository.SubscriptionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Everything RazorpayWebhookController needs done to a verified webhook payload - kept out of the controller
 * itself so the controller never touches entities directly (ModuleBoundariesTest.controllersDoNotExposeEntities).
 *
 * IMPORTANT - not exercised against real Razorpay webhook delivery: that needs a public HTTPS URL Razorpay can
 * reach (a local machine needs a tunnel, e.g. ngrok) and a live subscription to trigger real events, neither of
 * which was available while this was written. The event/field names below follow Razorpay's published webhook
 * reference (https://razorpay.com/docs/webhooks/payloads/subscriptions/) as of when this was written - verify
 * a real payload (Razorpay's dashboard can replay a sample event) before relying on this in production.
 */
@Service
public class RazorpayWebhookService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookService.class);

    private final RazorpaySignatureVerifier signatureVerifier;
    private final PavitraProperties properties;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public RazorpayWebhookService(
            RazorpaySignatureVerifier signatureVerifier,
            PavitraProperties properties,
            SubscriptionRepository subscriptionRepository,
            PaymentService paymentService,
            ObjectMapper objectMapper
    ) {
        this.signatureVerifier = signatureVerifier;
        this.properties = properties;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    /** Returns false only when the signature itself doesn't verify - the controller turns that into a 401 so a
     *  misconfigured secret is visible in Razorpay's own delivery logs. Everything else (unknown event type,
     *  unknown subscription id) is handled and still reports true, so Razorpay doesn't retry an event that will
     *  never resolve. */
    @Transactional
    public boolean handle(String rawBody, String signature) {
        String secret = properties.getRazorpay().getWebhookSecret();
        if (!signatureVerifier.verify(rawBody, signature, secret)) {
            log.warn("Rejected a Razorpay webhook with an invalid signature");
            return false;
        }

        JsonNode event = objectMapper.readTree(rawBody);
        String eventType = event.path("event").asText("");
        switch (eventType) {
            case "subscription.charged" -> handleCharged(event);
            case "payment.failed" -> handleFailed(event);
            case "subscription.cancelled", "subscription.completed" -> handleEnded(event);
            default -> log.info("Ignoring Razorpay webhook event type: {}", eventType);
        }
        return true;
    }

    private void handleCharged(JsonNode event) {
        String providerSubscriptionId = event.path("payload").path("subscription").path("entity").path("id").asText(null);
        JsonNode paymentEntity = event.path("payload").path("payment").path("entity");
        String providerPaymentId = paymentEntity.path("id").asText(null);
        long amountInSmallestUnit = paymentEntity.path("amount").asLong(0);
        String currencyCode = paymentEntity.path("currency").asText(null);

        withSubscription(providerSubscriptionId, subscription -> paymentService.confirmCharge(
                subscription, providerPaymentId, fromSmallestUnit(amountInSmallestUnit), currencyCode));
    }

    private void handleFailed(JsonNode event) {
        JsonNode paymentEntity = event.path("payload").path("payment").path("entity");
        String providerSubscriptionId = paymentEntity.path("subscription_id").asText(null);
        String reason = paymentEntity.path("error_description").asText("Payment failed");

        withSubscription(providerSubscriptionId, subscription -> paymentService.recordFailedCharge(subscription, reason));
    }

    private void handleEnded(JsonNode event) {
        String providerSubscriptionId = event.path("payload").path("subscription").path("entity").path("id").asText(null);
        withSubscription(providerSubscriptionId, subscription -> {
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            subscription.setAutoRenew(false);
            subscription.setCancelledAt(Instant.now());
            subscription.setUpdatedAt(Instant.now());
            subscriptionRepository.save(subscription);
        });
    }

    private void withSubscription(String providerSubscriptionId, Consumer<Subscription> action) {
        if (providerSubscriptionId == null || providerSubscriptionId.isBlank()) {
            log.warn("Razorpay webhook event had no subscription id to correlate against");
            return;
        }
        Optional<Subscription> subscription = subscriptionRepository.findByProviderSubscriptionId(providerSubscriptionId);
        if (subscription.isEmpty()) {
            log.warn("Razorpay webhook referenced unknown subscription id {}", providerSubscriptionId);
            return;
        }
        action.accept(subscription.get());
    }

    private BigDecimal fromSmallestUnit(long amount) {
        return BigDecimal.valueOf(amount).movePointLeft(2);
    }
}
