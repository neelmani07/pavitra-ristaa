package com.pavitraristaa.auth.service;

public record SocialIdentity(String provider, String subject, String email, boolean emailVerified) {
}
