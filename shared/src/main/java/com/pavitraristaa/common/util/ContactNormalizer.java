package com.pavitraristaa.common.util;

public final class ContactNormalizer {

    private ContactNormalizer() {
    }

    public static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    public static String normalizeMobile(String mobile) {
        if (mobile == null || mobile.isBlank()) {
            return null;
        }
        String trimmed = mobile.trim();
        return trimmed.replaceAll("[\\s-()]", "");
    }
}
