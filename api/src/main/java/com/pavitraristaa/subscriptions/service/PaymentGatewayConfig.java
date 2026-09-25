package com.pavitraristaa.subscriptions.service;

import com.pavitraristaa.config.PavitraProperties;
import com.pavitraristaa.subscriptions.repository.PlanRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.web.client.RestClient;

/**
 * Exactly one PaymentGateway bean exists at a time: RazorpayPaymentGateway when RAZORPAY_KEY_ID is set (see
 * .env.example), the stub (AutoApprovePaymentGateway) otherwise - which is every environment until a real
 * Razorpay account is configured, including every test in this codebase. Neither implementation is @Component
 * itself, specifically so this class is the one place that decides which is active.
 */
@Configuration
public class PaymentGatewayConfig {

    @Bean
    @Conditional(RazorpayKeyConfigured.class)
    public PaymentGateway razorpayPaymentGateway(RestClient restClient, PavitraProperties properties, PlanRepository planRepository) {
        return new RazorpayPaymentGateway(restClient, properties, planRepository);
    }

    @Bean
    @ConditionalOnMissingBean(PaymentGateway.class)
    public PaymentGateway stubPaymentGateway() {
        return new AutoApprovePaymentGateway();
    }

    /**
     * @ConditionalOnProperty(name = "pavitra.razorpay.key-id") alone doesn't work here: application.yml binds
     * that property with a "${RAZORPAY_KEY_ID:}" empty-string default, so the property always resolves as
     * "present" even when unset - @ConditionalOnProperty without an explicit havingValue only excludes a
     * literal "false", not blank. This checks the actual resolved value is non-blank instead.
     */
    static class RazorpayKeyConfigured implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String keyId = context.getEnvironment().getProperty("pavitra.razorpay.key-id", "");
            return keyId != null && !keyId.isBlank();
        }
    }
}
