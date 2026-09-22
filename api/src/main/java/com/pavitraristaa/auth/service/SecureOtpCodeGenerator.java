package com.pavitraristaa.auth.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class SecureOtpCodeGenerator implements OtpCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String generateNumericCode(int length) {
        int bound = (int) Math.pow(10, length);
        int min = bound / 10;
        int value = RANDOM.nextInt(bound - min) + min;
        return Integer.toString(value);
    }
}
