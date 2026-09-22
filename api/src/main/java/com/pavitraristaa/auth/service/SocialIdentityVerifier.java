package com.pavitraristaa.auth.service;

public interface SocialIdentityVerifier {

    SocialIdentity verifyGoogle(String idToken);

    SocialIdentity verifyApple(String identityToken);
}
