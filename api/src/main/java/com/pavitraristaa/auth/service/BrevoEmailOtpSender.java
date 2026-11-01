package com.pavitraristaa.auth.service;

import com.pavitraristaa.auth.entity.OtpPurpose;
import com.pavitraristaa.config.PavitraProperties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sends the OTP as a transactional email through Brevo's REST API (POST /v3/smtp/email; a 201 with a messageId means
 * it was accepted). Plain RestClient, no Brevo SDK - same approach as the Razorpay gateway.
 *
 * <p>A failed send is logged and swallowed rather than thrown. The account or OTP has already been saved, the user
 * can ask for another code, and a flaky email provider must not turn a successful registration into a 500. The
 * cost is that a bad API key shows up as an ERROR line here instead of an error to the user, so watch the logs
 * after changing Brevo settings. Neither the code nor the full address is ever logged.
 */
public class BrevoEmailOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailOtpSender.class);
    private static final ObjectMapper JSON = JsonMapper.builder().build();

    private final RestClient restClient;
    private final PavitraProperties properties;

    public BrevoEmailOtpSender(RestClient restClient, PavitraProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public void send(String destination, OtpPurpose purpose, String code) {
        PavitraProperties.Brevo brevo = properties.getBrevo();
        OtpEmailTemplate.Content content =
                OtpEmailTemplate.forPurpose(purpose, code, properties.getOtp().getTtl().toMinutes());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sender", Map.of("name", brevo.getSenderName(), "email", brevo.getSenderEmail()));
        body.put("to", List.of(Map.of("email", destination)));
        body.put("subject", content.subject());
        body.put("htmlContent", content.html());
        body.put("textContent", content.text());
        try {
            restClient.post()
                    .uri(brevo.getBaseUrl() + "/v3/smtp/email")
                    .header("api-key", brevo.getApiKey())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    // Pre-serialised so the request carries an exact Content-Length. Handing RestClient the map would
                    // stream it chunked, which some API gateways and simple servers refuse (411 / no response).
                    .body(JSON.writeValueAsBytes(body))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            log.error("Brevo rejected the {} OTP email to {} (HTTP {}): {}", purpose, mask(destination),
                    exception.getStatusCode().value(), abbreviate(exception.getResponseBodyAsString()));
        } catch (RestClientException exception) {
            log.error("Could not reach Brevo to send the {} OTP email to {}: {}", purpose, mask(destination),
                    exception.getMessage());
        }
    }

    /** ada.lovelace@example.com -> a***@example.com: enough to tell cases apart in a log, not enough to harvest. */
    static String mask(String email) {
        int at = email.indexOf('@');
        return at <= 0 ? "***" : email.charAt(0) + "***" + email.substring(at);
    }

    private static String abbreviate(String text) {
        return text == null ? "" : text.length() > 300 ? text.substring(0, 300) + "..." : text;
    }
}
