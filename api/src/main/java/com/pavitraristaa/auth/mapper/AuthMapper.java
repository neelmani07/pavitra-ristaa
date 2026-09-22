package com.pavitraristaa.auth.mapper;

import com.pavitraristaa.auth.dto.AccountResponse;
import com.pavitraristaa.auth.dto.RegisterResponse;
import com.pavitraristaa.auth.dto.SessionResponse;
import com.pavitraristaa.auth.dto.TokenResponse;
import com.pavitraristaa.auth.entity.RefreshToken;
import com.pavitraristaa.auth.entity.UserAccount;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public RegisterResponse toRegisterResponse(UserAccount user) {
        return new RegisterResponse(user.getUuid(), user.getAccountStatus().name(), true);
    }

    public AccountResponse toAccountResponse(UserAccount user, List<String> roles) {
        return new AccountResponse(
                user.getUuid(),
                user.getEmail(),
                user.getMobile(),
                user.getAccountStatus().name(),
                user.isEmailVerified(),
                user.isMobileVerified(),
                roles,
                user.getCreatedAt()
        );
    }

    public TokenResponse toTokenResponse(String accessToken, String refreshToken, Instant expiresAt) {
        return new TokenResponse(accessToken, refreshToken, expiresAt, "Bearer");
    }

    public SessionResponse toSessionResponse(RefreshToken token, Long currentSessionId) {
        return new SessionResponse(
                token.getId(),
                token.getDeviceName(),
                token.getPlatform() == null ? null : token.getPlatform().name(),
                token.getCreatedAt(),
                token.getLastUsedAt(),
                token.getExpiresAt(),
                currentSessionId != null && currentSessionId.equals(token.getId())
        );
    }
}
