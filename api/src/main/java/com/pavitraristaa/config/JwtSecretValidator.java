package com.pavitraristaa.config;

import com.pavitraristaa.config.PavitraProperties.Jwt;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class JwtSecretValidator {

    private final PavitraProperties properties;
    private final Environment environment;

    public JwtSecretValidator(PavitraProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        Jwt jwt = properties.getSecurity().getJwt();
        boolean production = java.util.Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (jwt.getSecret() == null || jwt.getSecret().isBlank() || jwt.getSecret().length() < 32) {
            if (production) {
                throw new IllegalStateException("JWT_SECRET must be set to at least 32 characters in production");
            }
        }
        if (production && jwt.getSecret().startsWith("change-me-local-dev")) {
            throw new IllegalStateException("Production must not use the local JWT secret");
        }
    }
}
