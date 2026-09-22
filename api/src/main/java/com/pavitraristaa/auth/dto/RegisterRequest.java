package com.pavitraristaa.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email String email,
        @Size(max = 20) String mobile,
        @NotBlank @Size(min = 8, max = 72) String password,
        String referralCode,
        String inviteCode,
        Boolean acceptedTerms,
        Boolean acceptedPrivacyPolicy
) {
}
