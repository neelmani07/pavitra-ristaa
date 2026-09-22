package com.pavitraristaa.auth.service;

import com.pavitraristaa.auth.entity.OtpPurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingOtpSender.class);

    @Override
    public void send(String destination, OtpPurpose purpose, String code) {
        log.info("OTP dispatched for purpose {} to a {} destination", purpose, destinationType(destination));
    }

    private String destinationType(String destination) {
        return destination.contains("@") ? "email" : "mobile";
    }
}
