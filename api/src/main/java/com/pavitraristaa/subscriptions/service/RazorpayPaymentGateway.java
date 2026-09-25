package com.pavitraristaa.subscriptions.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.config.PavitraProperties;
import com.pavitraristaa.subscriptions.entity.BillingPeriod;
import com.pavitraristaa.subscriptions.entity.Plan;
import com.pavitraristaa.subscriptions.repository.PlanRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/**
 * Talks to Razorpay's Subscriptions API (recurring billing via UPI Autopay / card e-mandate) using the same
 * plain RestClient + HTTP Basic auth pattern GoogleAppleIdentityVerifier already uses for OAuth - no SDK
 * dependency added, since Razorpay's REST API needs nothing an SDK would provide beyond auth-header plumbing.
 *
 * IMPORTANT - not exercised against the real Razorpay API in this codebase: no account/test keys were
 * available while this was written. The request/response shapes below follow Razorpay's published API
 * reference (https://razorpay.com/docs/api/payments/subscriptions/) as of when this was written; verify them
 * against a real test-mode call before going live, and adjust field names here if Razorpay's actual response
 * differs. RazorpaySignatureVerifier (the part that can be tested without an account) has its own unit test.
 */
class RazorpayPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentGateway.class);
    private static final String BASE_URL = "https://api.razorpay.com/v1";
    // Razorpay requires a positive total_count for a subscription (no literal "until cancelled"); this is
    // effectively unbounded for any real billing period (120 monthly cycles = 10 years, 120 yearly = 120 years)
    // and the mandate is cancelled outright via cancelSubscription() rather than left to run out.
    private static final int EFFECTIVELY_UNLIMITED_CYCLES = 120;

    private final RestClient restClient;
    private final PavitraProperties properties;
    private final PlanRepository planRepository;

    RazorpayPaymentGateway(RestClient restClient, PavitraProperties properties, PlanRepository planRepository) {
        this.restClient = restClient;
        this.properties = properties;
        this.planRepository = planRepository;
    }

    @Override
    public String providerName() {
        return "RAZORPAY";
    }

    @Override
    public String checkoutKeyId() {
        return properties.getRazorpay().getKeyId();
    }

    @Override
    public boolean supportsPerSubscriptionDiscount() {
        return false;
    }

    @Override
    public SubscriptionCheckout createSubscriptionCheckout(UserAccount user, Plan plan) {
        String providerPlanId = ensureProviderPlan(plan);
        JsonNode response = post("/subscriptions", Map.of(
                "plan_id", providerPlanId,
                "total_count", EFFECTIVELY_UNLIMITED_CYCLES,
                "quantity", 1,
                "customer_notify", 1,
                "notes", Map.of("pavitraUserId", user.getUuid().toString(), "pavitraPlanCode", plan.getCode())));
        String subscriptionId = textOrThrow(response, "id", "Razorpay subscription response had no id");
        return new SubscriptionCheckout(subscriptionId, false);
    }

    @Override
    public void cancelSubscription(String providerSubscriptionId) {
        try {
            post("/subscriptions/" + providerSubscriptionId + "/cancel", Map.of("cancel_at_cycle_end", 0));
        } catch (RestClientException exception) {
            // Cancelling something Razorpay no longer recognizes (already cancelled/completed there, or the id
            // is from before an account reset) shouldn't block our own side from being marked CANCELLED - log
            // and move on rather than leaving the user's cancel request stuck.
            log.warn("Could not cancel Razorpay subscription {} at the provider: {}", providerSubscriptionId, exception.getMessage());
        }
    }

    /** Creates the provider-side plan once and caches its id on our own Plan row; every later checkout reuses it. */
    private String ensureProviderPlan(Plan plan) {
        if (plan.getProviderPlanId() != null && !plan.getProviderPlanId().isBlank()) {
            return plan.getProviderPlanId();
        }
        PeriodAndInterval cycle = cycleOf(plan.getBillingPeriod());
        JsonNode response = post("/plans", Map.of(
                "period", cycle.period(),
                "interval", cycle.interval(),
                "item", Map.of(
                        "name", plan.getName(),
                        "amount", toSmallestUnit(plan.getPrice()),
                        "currency", plan.getCurrencyCode())));
        String providerPlanId = textOrThrow(response, "id", "Razorpay plan response had no id");
        plan.setProviderPlanId(providerPlanId);
        planRepository.save(plan);
        return providerPlanId;
    }

    private PeriodAndInterval cycleOf(BillingPeriod billingPeriod) {
        return switch (billingPeriod) {
            case MONTHLY -> new PeriodAndInterval("monthly", 1);
            // Razorpay has no native "quarterly" period - charge every 3rd month instead.
            case QUARTERLY -> new PeriodAndInterval("monthly", 3);
            case YEARLY -> new PeriodAndInterval("yearly", 1);
            case ONE_TIME -> throw new ApiException(
                    ErrorCode.VALIDATION_ERROR, "This plan is not billed on a recurring cycle and cannot use subscription checkout");
        };
    }

    /** Razorpay amounts are in the smallest currency unit (paise for INR) - price 999.00 becomes 99900. */
    private long toSmallestUnit(BigDecimal price) {
        return price.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private JsonNode post(String path, Map<String, ?> body) {
        String keyId = properties.getRazorpay().getKeyId();
        String keySecret = properties.getRazorpay().getKeySecret();
        String credentials = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
        try {
            return restClient.post()
                    .uri(BASE_URL + path)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            log.warn("Razorpay API call to {} failed: {}", path, exception.getMessage());
            throw new ApiException(ErrorCode.PAYMENT_FAILED, "Could not reach the payment provider");
        }
    }

    private String textOrThrow(JsonNode response, String field, String message) {
        String value = response == null ? null : response.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw new ApiException(ErrorCode.PAYMENT_FAILED, message);
        }
        return value;
    }

    private record PeriodAndInterval(String period, int interval) {
    }
}
