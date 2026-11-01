package com.pavitraristaa.auth.service;

import com.pavitraristaa.auth.entity.OtpPurpose;

/**
 * Picks the channel from the destination: an address with an "@" goes to the email sender, everything else (a
 * phone number) to the mobile sender. Adding SMS delivery later means passing a different mobile sender in
 * OtpSenderConfig; nothing else changes.
 */
public class DestinationRoutingOtpSender implements OtpSender {

    private final OtpSender emailSender;
    private final OtpSender mobileSender;

    public DestinationRoutingOtpSender(OtpSender emailSender, OtpSender mobileSender) {
        this.emailSender = emailSender;
        this.mobileSender = mobileSender;
    }

    @Override
    public void send(String destination, OtpPurpose purpose, String code) {
        (destination.contains("@") ? emailSender : mobileSender).send(destination, purpose, code);
    }
}
