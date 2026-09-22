package com.pavitraristaa.auth.service;

import com.pavitraristaa.auth.dto.SessionResponse;
import com.pavitraristaa.auth.dto.TokenResponse;
import com.pavitraristaa.auth.entity.DevicePlatform;
import com.pavitraristaa.auth.entity.RefreshToken;
import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.mapper.AuthMapper;
import com.pavitraristaa.auth.repository.RefreshTokenRepository;
import com.pavitraristaa.auth.repository.UserRoleRepository;
import com.pavitraristaa.auth.security.JwtService;
import com.pavitraristaa.common.api.PagedData;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.util.Hashing;
import com.pavitraristaa.config.PavitraProperties;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRoleRepository userRoleRepository;
    private final JwtService jwtService;
    private final AuthMapper authMapper;
    private final PavitraProperties properties;

    public SessionTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRoleRepository userRoleRepository,
            JwtService jwtService,
            AuthMapper authMapper,
            PavitraProperties properties
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRoleRepository = userRoleRepository;
        this.jwtService = jwtService;
        this.authMapper = authMapper;
        this.properties = properties;
    }

    @Transactional
    public TokenResponse issueTokens(UserAccount user, String deviceName, DevicePlatform platform, boolean rememberMe) {
        Instant now = Instant.now();
        String rawRefresh = Hashing.randomUrlSafeToken(32);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(Hashing.sha256Hex(rawRefresh));
        refreshToken.setDeviceName(deviceName);
        refreshToken.setPlatform(platform);
        refreshToken.setCreatedAt(now);
        refreshToken.setLastUsedAt(now);
        refreshToken.setExpiresAt(now.plus(rememberMe
                ? properties.getSecurity().getJwt().getRememberMeRefreshTokenTtl()
                : properties.getSecurity().getJwt().getRefreshTokenTtl()));
        refreshToken.setRememberMe(rememberMe);
        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        List<String> roles = rolesOf(user);
        String accessToken = jwtService.createAccessToken(user.getUuid(), saved.getId(), roles);
        return authMapper.toTokenResponse(accessToken, rawRefresh, jwtService.accessTokenExpiresAt());
    }

    @Transactional
    public TokenResponse rotate(String rawRefreshToken) {
        RefreshToken existing = requireActive(rawRefreshToken);
        existing.setRevokedAt(Instant.now());
        refreshTokenRepository.save(existing);
        // Carry the original session's remember-me choice forward, or a refresh silently shortens a 90-day
        // remember-me session to 30 days the first time the client renews its access token.
        return issueTokens(existing.getUser(), existing.getDeviceName(), existing.getPlatform(), existing.isRememberMe());
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(Hashing.sha256Hex(rawRefreshToken))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> {
                    token.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(token);
                });
    }

    @Transactional
    public void revokeById(UserAccount user, Long sessionId) {
        RefreshToken token = refreshTokenRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Session not found"));
        if (!token.getUser().getId().equals(user.getId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Cannot revoke another user's session");
        }
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);
    }

    @Transactional
    public void revokeAll(UserAccount user) {
        refreshTokenRepository.revokeAllActive(user, Instant.now());
    }

    @Transactional
    public void revokeAllExcept(UserAccount user, Long keepSessionId) {
        if (keepSessionId == null) {
            revokeAll(user);
            return;
        }
        refreshTokenRepository.revokeAllActiveExcept(user, keepSessionId, Instant.now());
    }

    @Transactional(readOnly = true)
    public PagedData<SessionResponse> listSessions(UserAccount user, Long currentSessionId, Pageable pageable) {
        Page<RefreshToken> page = refreshTokenRepository.findByUserAndRevokedAtIsNullAndExpiresAtAfter(
                user, Instant.now(), pageable);
        List<SessionResponse> items = page.getContent().stream()
                .map(token -> authMapper.toSessionResponse(token, currentSessionId))
                .toList();
        return new PagedData<>(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    public RefreshToken requireActive(String rawRefreshToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(Hashing.sha256Hex(rawRefreshToken))
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_REVOKED, "Refresh token is invalid"));
        if (token.getRevokedAt() != null) {
            throw new ApiException(ErrorCode.SESSION_REVOKED, "Session has been revoked");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.SESSION_EXPIRED, "Session has expired");
        }
        token.setLastUsedAt(Instant.now());
        return refreshTokenRepository.save(token);
    }

    public List<String> rolesOf(UserAccount user) {
        return userRoleRepository.findByUserWithRole(user).stream()
                .map(userRole -> userRole.getRole().getCode())
                .toList();
    }
}
