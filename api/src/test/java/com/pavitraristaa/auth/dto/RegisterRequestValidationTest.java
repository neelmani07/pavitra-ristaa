package com.pavitraristaa.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

/**
 * The service-level tests call AuthService directly and so never run bean validation; this is what a real HTTP
 * request goes through first, which is where a padded referral code was once rejected before the service could trim it.
 */
class RegisterRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void referralCodeToleratesSurroundingWhitespaceButNotOtherCharacters() {
        assertThat(violationsFor(null)).isEmpty();
        assertThat(violationsFor("")).isEmpty();
        assertThat(violationsFor("   ")).isEmpty();
        assertThat(violationsFor("ab-12")).isEmpty();
        assertThat(violationsFor("  ab_12  ")).isEmpty();

        assertThat(violationsFor("ab 12")).isNotEmpty();
        assertThat(violationsFor("ab12!")).isNotEmpty();
        assertThat(violationsFor("a".repeat(65))).isNotEmpty();
    }

    private java.util.Set<?> violationsFor(String referralCode) {
        return validator.validate(new RegisterRequest("a@example.com", null, "password1", referralCode, true, true));
    }
}
