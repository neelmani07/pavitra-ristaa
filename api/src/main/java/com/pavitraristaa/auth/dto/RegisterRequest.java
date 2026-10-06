package com.pavitraristaa.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email String email,
        @Size(max = 20) String mobile,
        @NotBlank @Size(min = 8, max = 72) String password,
        // Surrounding whitespace is tolerated here and trimmed by the service; only the code itself is checked.
        @Size(max = 64) @Pattern(regexp = "^\\s*[A-Za-z0-9_-]*\\s*$", message = "referralCode may only contain letters, digits, '-' and '_'")
        String referralCode,
        Boolean acceptedTerms,
        Boolean acceptedPrivacyPolicy
) {
}
