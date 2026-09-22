package com.pavitraristaa.auth.service;

import com.pavitraristaa.auth.entity.OtpPurpose;

public interface OtpSender {

    void send(String destination, OtpPurpose purpose, String code);
}
