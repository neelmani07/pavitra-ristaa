package com.pavitraristaa.subscriptions.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/**
 * The one piece of the Razorpay integration this codebase can verify without a live account: pure HMAC-SHA256
 * math against a synthetic secret and payload. No real Razorpay signature was available to test against while
 * this was written - see RazorpayWebhookService's class comment.
 */
class RazorpaySignatureVerifierTest {

    private final RazorpaySignatureVerifier verifier = new RazorpaySignatureVerifier();

    @Test
    void acceptsACorrectlyComputedSignature() {
        String secret = "whsec_test_secret";
        String payload = "{\"event\":\"subscription.charged\"}";

        assertThat(verifier.verify(payload, sign(payload, secret), secret)).isTrue();
    }

    @Test
    void rejectsATamperedPayload() {
        String secret = "whsec_test_secret";
        String originalPayload = "{\"event\":\"subscription.charged\"}";
        String signature = sign(originalPayload, secret);

        assertThat(verifier.verify("{\"event\":\"subscription.cancelled\"}", signature, secret)).isFalse();
    }

    @Test
    void rejectsTheWrongSecret() {
        String payload = "{\"event\":\"subscription.charged\"}";
        String signature = sign(payload, "whsec_real_secret");

        assertThat(verifier.verify(payload, signature, "whsec_a_different_secret")).isFalse();
    }

    @Test
    void isCaseInsensitiveOnTheSignatureHexEncoding() {
        String secret = "whsec_test_secret";
        String payload = "{\"event\":\"payment.failed\"}";
        String signature = sign(payload, secret);

        assertThat(verifier.verify(payload, signature.toUpperCase(java.util.Locale.ROOT), secret)).isTrue();
    }

    @Test
    void rejectsWhenNoWebhookSecretIsConfigured() {
        String payload = "{\"event\":\"subscription.charged\"}";

        assertThat(verifier.verify(payload, sign(payload, "whsec_test_secret"), "")).isFalse();
    }

    /** Computes a signature the exact same way the verifier does, independently, so the test isn't just
     *  checking the implementation against itself. */
    private String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
