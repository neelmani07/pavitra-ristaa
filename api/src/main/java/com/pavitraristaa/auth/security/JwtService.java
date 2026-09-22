package com.pavitraristaa.auth.security;

import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.config.PavitraProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final PavitraProperties properties;

    public JwtService(PavitraProperties properties) {
        this.properties = properties;
    }

    public String createAccessToken(UUID userUuid, Long sessionId, List<String> roles) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getSecurity().getJwt().getAccessTokenTtl());
        return Jwts.builder()
                .issuer(properties.getSecurity().getJwt().getIssuer())
                .subject(userUuid.toString())
                .claim("sid", sessionId)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey())
                .compact();
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey())
                    .requireIssuer(properties.getSecurity().getJwt().getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException exception) {
            throw exception;
        }
    }

    public Instant accessTokenExpiresAt() {
        return Instant.now().plus(properties.getSecurity().getJwt().getAccessTokenTtl());
    }

    public String createEmailVerificationToken(UUID userUuid) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.getSecurity().getJwt().getIssuer())
                .subject(userUuid.toString())
                .claim("typ", "email_verify")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofHours(24))))
                .signWith(signingKey())
                .compact();
    }

    public UUID parseEmailVerificationToken(String token) {
        try {
            Claims claims = parse(token);
            if (!"email_verify".equals(claims.get("typ", String.class))) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid email verification token");
            }
            return UUID.fromString(claims.getSubject());
        } catch (ExpiredJwtException exception) {
            throw new ApiException(ErrorCode.OTP_EXPIRED, "Email verification token expired");
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid email verification token");
        }
    }

    private SecretKey signingKey() {
        byte[] keyBytes = properties.getSecurity().getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
