package com.pavitraristaa.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.pavitraristaa.auth.entity.OtpPurpose;
import com.pavitraristaa.config.PavitraProperties;
import org.junit.jupiter.api.Test;

class OtpSenderConfigTest {

    private final OtpSenderConfig config = new OtpSenderConfig();
    private final OtpSender fallback = mock(OtpSender.class);

    @Test
    void withoutAnApiKeyEmailStaysOnTheFallbackSoNothingIsSent() {
        assertThat(config.emailSender(new PavitraProperties(), fallback)).isSameAs(fallback);
    }

    @Test
    void anApiKeyWithoutASenderAddressStopsStartupInsteadOfFailingEveryEmailLater() {
        PavitraProperties properties = new PavitraProperties();
        properties.getBrevo().setApiKey("some-key");

        assertThatThrownBy(() -> config.emailSender(properties, fallback))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BREVO_SENDER_EMAIL");
    }

    @Test
    void aFullyConfiguredBrevoIsUsedForEmail() {
        PavitraProperties properties = new PavitraProperties();
        properties.getBrevo().setApiKey("some-key");
        properties.getBrevo().setSenderEmail("no-reply@pavitra.example");

        assertThat(config.emailSender(properties, fallback)).isInstanceOf(BrevoEmailOtpSender.class);
    }

    @Test
    void theRouterSendsEmailsAndMobileNumbersDownDifferentChannels() {
        OtpSender email = mock(OtpSender.class);
        OtpSender mobile = mock(OtpSender.class);
        DestinationRoutingOtpSender router = new DestinationRoutingOtpSender(email, mobile);

        router.send("ada@example.com", OtpPurpose.REGISTER, "111111");
        router.send("+919876543210", OtpPurpose.REGISTER, "222222");

        verify(email).send("ada@example.com", OtpPurpose.REGISTER, "111111");
        verify(mobile).send("+919876543210", OtpPurpose.REGISTER, "222222");
        verifyNoInteractions(fallback);
    }
}
