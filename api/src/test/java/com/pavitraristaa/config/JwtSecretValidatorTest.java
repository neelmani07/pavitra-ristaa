package com.pavitraristaa.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * JwtService silently zero-pads a short/blank secret into a predictable HMAC key, and JwtAuthenticationFilter
 * trusts the resulting token's claims outright, so this must fail startup in every profile - not just "prod".
 */
class JwtSecretValidatorTest {

    @Test
    void rejectsBlankSecret() {
        assertRejected("");
    }

    @Test
    void rejectsSecretShorterThan32Characters() {
        assertRejected("short-secret");
    }

    @Test
    void rejectsTheLocalDevPlaceholder() {
        assertRejected("change-me-local-dev-jwt-secret-key-32");
    }

    @Test
    void acceptsAGenuineThirtyTwoCharacterSecret() {
        PavitraProperties properties = withSecret("a-generated-secret-that-is-long-enough");

        assertThatCode(() -> new JwtSecretValidator(properties).validate()).doesNotThrowAnyException();
    }

    private void assertRejected(String secret) {
        PavitraProperties properties = withSecret(secret);

        assertThatThrownBy(() -> new JwtSecretValidator(properties).validate())
                .isInstanceOf(IllegalStateException.class);
    }

    private PavitraProperties withSecret(String secret) {
        PavitraProperties properties = new PavitraProperties();
        properties.getSecurity().getJwt().setSecret(secret);
        return properties;
    }
}
