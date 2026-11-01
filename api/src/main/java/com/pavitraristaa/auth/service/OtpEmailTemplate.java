package com.pavitraristaa.auth.service;

import com.pavitraristaa.auth.entity.OtpPurpose;
import org.springframework.web.util.HtmlUtils;

/** The words in an OTP email. Kept apart from the sender so the wording can change without touching delivery. */
final class OtpEmailTemplate {

    record Content(String subject, String text, String html) {
    }

    private OtpEmailTemplate() {
    }

    static Content forPurpose(OtpPurpose purpose, String code, long validMinutes) {
        String subject;
        String lead;
        String ignore;
        switch (purpose) {
            case REGISTER -> {
                subject = "Verify your Pavitra Ristaa account";
                lead = "Your Pavitra Ristaa verification code is:";
                ignore = "If you did not create an account, you can ignore this email.";
            }
            case LOGIN -> {
                subject = "Your Pavitra Ristaa login code";
                lead = "Your Pavitra Ristaa login code is:";
                ignore = "If you did not try to sign in, you can ignore this email.";
            }
            case FORGOT_PASSWORD -> {
                subject = "Reset your Pavitra Ristaa password";
                lead = "Use this code to reset your Pavitra Ristaa password:";
                ignore = "If you did not ask for this, ignore this email and your password stays as it is.";
            }
            default -> {
                subject = "Confirm your change on Pavitra Ristaa";
                lead = "Your Pavitra Ristaa confirmation code is:";
                ignore = "If you did not ask for this change, you can ignore this email.";
            }
        }
        String validity = "It is valid for " + validMinutes + " minutes. Never share it with anyone.";
        String text = lead + "\n\n" + code + "\n\n" + validity + "\n" + ignore + "\n";
        String html = "<div style=\"font-family:Arial,Helvetica,sans-serif;font-size:14px;color:#222\">"
                + "<p>" + HtmlUtils.htmlEscape(lead) + "</p>"
                + "<p style=\"font-size:24px;font-weight:bold;letter-spacing:4px;word-break:break-all\">"
                + HtmlUtils.htmlEscape(code) + "</p>"
                + "<p>" + HtmlUtils.htmlEscape(validity) + "</p>"
                + "<p style=\"color:#666\">" + HtmlUtils.htmlEscape(ignore) + "</p></div>";
        return new Content(subject, text, html);
    }
}
