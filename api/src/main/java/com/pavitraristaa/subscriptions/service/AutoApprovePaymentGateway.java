package com.pavitraristaa.subscriptions.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.subscriptions.entity.Payment;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stand-in until a real payment provider is wired up: every charge and every retry succeeds immediately, with
 * a synthetic provider payment id so the "provider" column is never null and never collides. No money moves,
 * no external call is made - this exists purely so the rest of the subscription/payment/invoice flow can be
 * built, tested and demoed today. provider is recorded as "STUB" (not a real provider name) so it's obvious in
 * data which payments never touched a real gateway.
 */
@Component
public class AutoApprovePaymentGateway implements PaymentGateway {

    static final String PROVIDER = "STUB";

    private static final Logger log = LoggerFactory.getLogger(AutoApprovePaymentGateway.class);

    @Override
    public GatewayResult charge(UserAccount user, BigDecimal amount, String currencyCode, String description) {
        log.info(
                "Stub payment gateway auto-approving a charge of {} {} for user {} ({}) - no real payment "
                        + "provider is configured yet",
                amount, currencyCode, user.getUuid(), description);
        return new GatewayResult(true, syntheticId(), null);
    }

    @Override
    public GatewayResult retry(Payment payment) {
        log.info("Stub payment gateway auto-approving retry of payment {}", payment.getUuid());
        return new GatewayResult(true, syntheticId(), null);
    }

    private String syntheticId() {
        return "STUB-" + UUID.randomUUID();
    }
}
