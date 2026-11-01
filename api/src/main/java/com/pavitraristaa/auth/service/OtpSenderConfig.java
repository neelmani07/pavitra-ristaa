package com.pavitraristaa.auth.service;

import com.pavitraristaa.config.PavitraProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * The one place that decides how OTPs leave the system. Email goes through Brevo when BREVO_API_KEY is set (see
 * .env.example) and is only logged otherwise - which is every environment until Brevo is configured, including
 * every test. Mobile numbers are still only logged until an SMS provider (MSG91) is added: pass it as the second
 * argument below.
 *
 * <p>Setting the API key without a sender address fails the startup on purpose. The alternative - starting fine and
 * having every OTP email rejected - is a problem you would only notice from user complaints.
 */
@Configuration
public class OtpSenderConfig {

    @Bean
    public OtpSender otpSender(PavitraProperties properties) {
        OtpSender logging = new LoggingOtpSender();
        return new DestinationRoutingOtpSender(emailSender(properties, logging), logging);
    }

    OtpSender emailSender(PavitraProperties properties, OtpSender fallback) {
        PavitraProperties.Brevo brevo = properties.getBrevo();
        if (brevo.getApiKey() == null || brevo.getApiKey().isBlank()) {
            return fallback;
        }
        if (brevo.getSenderEmail() == null || brevo.getSenderEmail().isBlank()) {
            throw new IllegalStateException(
                    "BREVO_API_KEY is set but BREVO_SENDER_EMAIL is not: set it to a sender address verified in Brevo");
        }
        return new BrevoEmailOtpSender(brevoClient(), properties);
    }

    /** Own client with timeouts: a hung email provider must not hold a request thread (or a registration) forever. */
    private RestClient brevoClient() {
        // HTTP/1.1 pinned: the JDK client otherwise tries an HTTP/2 upgrade on plain http, which proxies and simple
        // test servers do not always understand. Brevo's real endpoint is https and fine either way.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().requestFactory(factory).build();
    }
}
