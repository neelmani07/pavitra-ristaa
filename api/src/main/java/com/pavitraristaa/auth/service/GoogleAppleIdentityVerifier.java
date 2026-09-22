package com.pavitraristaa.auth.service;

import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.config.PavitraProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class GoogleAppleIdentityVerifier implements SocialIdentityVerifier {

    private static final String GOOGLE_TOKEN_INFO = "https://oauth2.googleapis.com/tokeninfo?id_token={token}";
    private static final String APPLE_KEYS = "https://appleid.apple.com/auth/keys";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final PavitraProperties properties;
    private final Map<String, CachedKey> appleKeys = new ConcurrentHashMap<>();

    public GoogleAppleIdentityVerifier(
            RestClient restClient,
            ObjectMapper objectMapper,
            PavitraProperties properties
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public SocialIdentity verifyGoogle(String idToken) {
        JsonNode payload;
        try {
            payload = restClient.get()
                    .uri(GOOGLE_TOKEN_INFO, idToken)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Google identity token is invalid");
        }
        if (payload == null || payload.path("sub").asText("").isBlank()) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Google identity token is invalid");
        }
        String audience = payload.path("aud").asText("");
        String expectedAudience = properties.getAuth().getOauth().getGoogleClientId();
        if (expectedAudience != null && !expectedAudience.isBlank() && !expectedAudience.equals(audience)) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Google identity token audience is invalid");
        }
        String email = blankToNull(payload.path("email").asText(null));
        boolean emailVerified = payload.path("email_verified").asBoolean(false)
                || "true".equalsIgnoreCase(payload.path("email_verified").asText());
        return new SocialIdentity("GOOGLE", payload.path("sub").asText(), email, emailVerified);
    }

    @Override
    public SocialIdentity verifyApple(String identityToken) {
        String expectedAudience = properties.getAuth().getOauth().getAppleClientId();
        if (expectedAudience == null || expectedAudience.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Apple Sign In is not configured");
        }
        String kid = readKid(identityToken);
        RSAPublicKey publicKey = appleKey(kid);
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(properties.getAuth().getOauth().getAppleIssuer())
                    .requireAudience(expectedAudience)
                    .build()
                    .parseSignedClaims(identityToken)
                    .getPayload();
            String email = blankToNull(claims.get("email", String.class));
            boolean emailVerified = Boolean.parseBoolean(String.valueOf(claims.getOrDefault("email_verified", false)));
            return new SocialIdentity("APPLE", claims.getSubject(), email, emailVerified);
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Apple identity token is invalid");
        }
    }

    private String readKid(String identityToken) {
        try {
            String[] parts = identityToken.split("\\.");
            if (parts.length < 2) {
                throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Apple identity token is invalid");
            }
            byte[] headerBytes = Base64.getUrlDecoder().decode(parts[0]);
            JsonNode header = objectMapper.readTree(headerBytes);
            String kid = header.path("kid").asText("");
            if (kid.isBlank()) {
                throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Apple identity token is invalid");
            }
            return kid;
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Apple identity token is invalid");
        }
    }

    private RSAPublicKey appleKey(String kid) {
        CachedKey cached = appleKeys.get(kid);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.key();
        }
        refreshAppleKeys();
        CachedKey refreshed = appleKeys.get(kid);
        if (refreshed == null) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Apple identity token is invalid");
        }
        return refreshed.key();
    }

    private void refreshAppleKeys() {
        try {
            JsonNode body = restClient.get()
                    .uri(APPLE_KEYS)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null || !body.has("keys")) {
                throw new ApiException(ErrorCode.INTERNAL_ERROR, "Unable to verify Apple identity token");
            }
            Instant expiresAt = Instant.now().plusSeconds(3_600);
            for (JsonNode keyNode : body.get("keys")) {
                String kid = keyNode.path("kid").asText();
                if (kid.isBlank()) {
                    continue;
                }
                appleKeys.put(kid, new CachedKey(toRsaKey(keyNode), expiresAt));
            }
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Unable to verify Apple identity token");
        }
    }

    private RSAPublicKey toRsaKey(JsonNode keyNode) throws Exception {
        byte[] modulus = Base64.getUrlDecoder().decode(keyNode.path("n").asText());
        byte[] exponent = Base64.getUrlDecoder().decode(keyNode.path("e").asText());
        RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(1, modulus), new BigInteger(1, exponent));
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record CachedKey(RSAPublicKey key, Instant expiresAt) {
    }
}
