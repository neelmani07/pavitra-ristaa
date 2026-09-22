package com.pavitraristaa.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.pavitraristaa.auth.entity.DevicePlatform;
import com.pavitraristaa.auth.entity.RefreshToken;
import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.mapper.AuthMapper;
import com.pavitraristaa.auth.repository.RefreshTokenRepository;
import com.pavitraristaa.auth.repository.UserRoleRepository;
import com.pavitraristaa.auth.security.JwtService;
import com.pavitraristaa.config.PavitraProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private JwtService jwtService;

    private SessionTokenService service;
    private UserAccount user;

    @BeforeEach
    void setUp() {
        service = new SessionTokenService(
                refreshTokenRepository, userRoleRepository, jwtService, new AuthMapper(), new PavitraProperties());
        user = new UserAccount();
        user.setId(9L);
        user.setUuid(UUID.randomUUID());
        when(jwtService.createAccessToken(any(), any(), any())).thenReturn("access-token");
        when(jwtService.accessTokenExpiresAt()).thenReturn(Instant.now().plusSeconds(900));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * Regression test: rotate() used to hardcode rememberMe=false on every refresh, so a 90-day remember-me
     * session silently shrank to the 30-day default the first time the client refreshed its access token.
     */
    @Test
    void rotatePreservesRememberMeFromTheOriginalSession() {
        RefreshToken existing = new RefreshToken();
        existing.setId(1L);
        existing.setUser(user);
        existing.setDeviceName("Pixel");
        existing.setPlatform(DevicePlatform.ANDROID);
        existing.setExpiresAt(Instant.now().plusSeconds(3600));
        existing.setRememberMe(true);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));

        service.rotate("raw-refresh-token");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        org.mockito.Mockito.verify(refreshTokenRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        RefreshToken reissued = captor.getAllValues().get(2);
        assertThat(reissued.isRememberMe()).isTrue();

        PavitraProperties.Jwt jwt = new PavitraProperties().getSecurity().getJwt();
        Duration untilExpiry = Duration.between(Instant.now(), reissued.getExpiresAt());
        assertThat(untilExpiry).isCloseTo(jwt.getRememberMeRefreshTokenTtl(), Duration.ofMinutes(1));
    }

    @Test
    void rotateKeepsAShortSessionShortWhenRememberMeWasNeverSet() {
        RefreshToken existing = new RefreshToken();
        existing.setId(1L);
        existing.setUser(user);
        existing.setExpiresAt(Instant.now().plusSeconds(3600));
        existing.setRememberMe(false);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));

        service.rotate("raw-refresh-token");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        org.mockito.Mockito.verify(refreshTokenRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        RefreshToken reissued = captor.getAllValues().get(2);
        assertThat(reissued.isRememberMe()).isFalse();

        PavitraProperties.Jwt jwt = new PavitraProperties().getSecurity().getJwt();
        Duration untilExpiry = Duration.between(Instant.now(), reissued.getExpiresAt());
        assertThat(untilExpiry).isCloseTo(jwt.getRefreshTokenTtl(), Duration.ofMinutes(1));
    }

    @Test
    void issueTokensRecordsTheRememberMeChoice() {
        service.issueTokens(user, "iPhone", DevicePlatform.IOS, true);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        org.mockito.Mockito.verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().isRememberMe()).isTrue();
    }
}
