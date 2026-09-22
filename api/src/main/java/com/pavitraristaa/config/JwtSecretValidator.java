package com.pavitraristaa.config;

import com.pavitraristaa.config.PavitraProperties.Jwt;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * Fails startup in every profile, not just "prod", if the JWT signing key is missing, too short, or the known
 * local-dev placeholder. JwtService silently zero-pads a short/blank secret into a predictable HMAC key, and
 * JwtAuthenticationFilter trusts the resulting token's subject/roles claims outright, so a weak key anywhere
 * reachable means full account and role impersonation. Only the "test" profile carries its own fixed secret
 * (application-test.yml) purely to keep tests deterministic; it is long enough to pass this check unaided.
 */
@Configuration
public class JwtSecretValidator {

    private static final String LOCAL_DEV_PLACEHOLDER_PREFIX = "change-me-local-dev";

    private final PavitraProperties properties;

    public JwtSecretValidator(PavitraProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void validate() {
        Jwt jwt = properties.getSecurity().getJwt();
        String secret = jwt.getSecret();
        if (secret == null || secret.isBlank() || secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set to at least 32 characters. Run ./scripts/setup-local.sh to generate one for local development.");
        }
        if (secret.startsWith(LOCAL_DEV_PLACEHOLDER_PREFIX)) {
            throw new IllegalStateException(
                    "JWT_SECRET must not use the local-dev placeholder value. Run ./scripts/setup-local.sh to generate a real one.");
        }
    }
}
