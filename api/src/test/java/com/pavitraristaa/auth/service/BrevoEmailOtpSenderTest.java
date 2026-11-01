package com.pavitraristaa.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.pavitraristaa.auth.entity.OtpPurpose;
import com.pavitraristaa.config.PavitraProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

/** Talks to a throwaway local HTTP server standing in for api.brevo.com, so the exact request is what is asserted. */
class BrevoEmailOtpSenderTest {

    private record Captured(String method, String path, String apiKey, String contentType, String contentLength,
                            String transferEncoding, String body) {
    }

    private HttpServer server;
    private final List<Captured> requests = new CopyOnWriteArrayList<>();
    private volatile int responseStatus = 201;
    private volatile String responseBody = "{\"messageId\":\"<202610270001.1@relay.example>\"}";
    private ListAppender<ILoggingEvent> logs;
    private Logger senderLogger;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            byte[] in = exchange.getRequestBody().readAllBytes();
            requests.add(new Captured(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("api-key"),
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    exchange.getRequestHeaders().getFirst("Content-Length"),
                    exchange.getRequestHeaders().getFirst("Transfer-Encoding"),
                    new String(in, StandardCharsets.UTF_8)));
            byte[] out = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
        senderLogger = (Logger) LoggerFactory.getLogger(BrevoEmailOtpSender.class);
        logs = new ListAppender<>();
        logs.start();
        senderLogger.addAppender(logs);
    }

    @AfterEach
    void stop() {
        senderLogger.detachAppender(logs);
        server.stop(0);
    }

    private BrevoEmailOtpSender sender(String baseUrl) {
        PavitraProperties properties = new PavitraProperties();
        properties.getBrevo().setApiKey("test-api-key");
        properties.getBrevo().setSenderEmail("no-reply@pavitra.example");
        properties.getBrevo().setBaseUrl(baseUrl);
        return new BrevoEmailOtpSender(RestClient.builder().build(), properties);
    }

    private BrevoEmailOtpSender sender() {
        return sender("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @Test
    void sendsTheCodeAsATransactionalEmailThroughBrevosApi() {
        sender().send("ada@example.com", OtpPurpose.REGISTER, "482913");

        assertThat(requests).hasSize(1);
        Captured request = requests.getFirst();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.path()).isEqualTo("/v3/smtp/email");
        assertThat(request.apiKey()).isEqualTo("test-api-key");
        assertThat(request.contentType()).startsWith("application/json");
        // An exact length, never chunked: some gateways answer a chunked request with 411 or nothing at all.
        assertThat(request.transferEncoding()).isNull();
        assertThat(request.contentLength()).isEqualTo(String.valueOf(request.body().getBytes(StandardCharsets.UTF_8).length));
        assertThat(request.body())
                .contains("\"to\":[{\"email\":\"ada@example.com\"}]")
                .contains("\"email\":\"no-reply@pavitra.example\"")
                .contains("\"name\":\"Pavitra Ristaa\"")
                .contains("\"subject\":\"Verify your Pavitra Ristaa account\"")
                .contains("\"htmlContent\"")
                .contains("\"textContent\"")
                .contains("482913")
                .contains("valid for 10 minutes");
        assertThat(logs.list).isEmpty();
    }

    @Test
    void passwordResetUsesItsOwnSubject() {
        sender().send("ada@example.com", OtpPurpose.FORGOT_PASSWORD, "tokenValue_-123");

        assertThat(requests.getFirst().body())
                .contains("Reset your Pavitra Ristaa password")
                .contains("tokenValue_-123");
    }

    @Test
    void aRejectedSendIsLoggedWithoutTheCodeOrTheFullAddressAndDoesNotThrow() {
        responseStatus = 400;
        responseBody = "{\"code\":\"unauthorized\",\"message\":\"Key not found\"}";

        assertThatCode(() -> sender().send("ada@example.com", OtpPurpose.REGISTER, "482913")).doesNotThrowAnyException();

        assertThat(requests).hasSize(1);
        assertThat(logs.list).hasSize(1);
        ILoggingEvent event = logs.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage())
                .contains("HTTP 400")
                .contains("Key not found")
                .contains("a***@example.com")
                .doesNotContain("482913")
                .doesNotContain("ada@example.com");
    }

    @Test
    void anUnreachableProviderIsLoggedAndDoesNotThrow() {
        // Nothing listens on port 1.
        assertThatCode(() -> sender("http://127.0.0.1:1").send("ada@example.com", OtpPurpose.LOGIN, "482913"))
                .doesNotThrowAnyException();

        assertThat(logs.list).hasSize(1);
        assertThat(logs.list.getFirst().getFormattedMessage())
                .contains("Could not reach Brevo")
                .doesNotContain("482913")
                .doesNotContain("ada@example.com");
    }

    @Test
    void maskKeepsOnlyTheFirstLetterAndTheDomain() {
        assertThat(BrevoEmailOtpSender.mask("ada.lovelace@example.com")).isEqualTo("a***@example.com");
        assertThat(BrevoEmailOtpSender.mask("not-an-email")).isEqualTo("***");
    }
}
