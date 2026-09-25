package com.pavitraristaa.subscriptions.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * HMAC-SHA256 verification for both webhook payloads (X-Razorpay-Signature header, verified against the
 * webhook secret) and the client-side checkout callback (order_id|payment_id, verified against the key secret)
 * - same algorithm, different secret and message shape, per Razorpay's own docs. Pure and stateless, so this is
 * testable with a synthetic secret and payload with no Razorpay account needed - see the unit test.
 */
@Component
public class RazorpaySignatureVerifier {

    private static final String ALGORITHM = "HmacSHA256";

    public boolean verify(String payload, String signature, String secret) {
        if (payload == null || signature == null || secret == null || secret.isBlank()) {
            return false;
        }
        String expected = hmacHex(payload, secret);
        return constantTimeEquals(expected, signature.trim().toLowerCase(java.util.Locale.ROOT));
    }

    private String hmacHex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("HmacSHA256 is not available", exception);
        }
    }

    /** Avoids a timing side-channel on signature comparison - MessageDigest.isEqual is constant-time. */
    private boolean constantTimeEquals(String a, String b) {
        return java.security.MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
