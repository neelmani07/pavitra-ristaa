package com.pavitraristaa.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pavitra")
public class PavitraProperties {

    private final Security security = new Security();
    private final Otp otp = new Otp();
    private final Auth auth = new Auth();
    private final Media media = new Media();
    private final Razorpay razorpay = new Razorpay();

    public Media getMedia() {
        return media;
    }

    public Security getSecurity() {
        return security;
    }

    public Otp getOtp() {
        return otp;
    }

    public Auth getAuth() {
        return auth;
    }

    public Razorpay getRazorpay() {
        return razorpay;
    }

    public static class Security {
        private final Jwt jwt = new Jwt();

        public Jwt getJwt() {
            return jwt;
        }
    }

    public static class Jwt {
        private String secret = "";
        private String issuer = "pavitra-ristaa";
        private Duration accessTokenTtl = Duration.ofMinutes(15);
        private Duration refreshTokenTtl = Duration.ofDays(30);
        private Duration rememberMeRefreshTokenTtl = Duration.ofDays(90);

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public Duration getAccessTokenTtl() {
            return accessTokenTtl;
        }

        public void setAccessTokenTtl(Duration accessTokenTtl) {
            this.accessTokenTtl = accessTokenTtl;
        }

        public Duration getRefreshTokenTtl() {
            return refreshTokenTtl;
        }

        public void setRefreshTokenTtl(Duration refreshTokenTtl) {
            this.refreshTokenTtl = refreshTokenTtl;
        }

        public Duration getRememberMeRefreshTokenTtl() {
            return rememberMeRefreshTokenTtl;
        }

        public void setRememberMeRefreshTokenTtl(Duration rememberMeRefreshTokenTtl) {
            this.rememberMeRefreshTokenTtl = rememberMeRefreshTokenTtl;
        }
    }

    public static class Otp {
        private Duration ttl = Duration.ofMinutes(10);
        private int length = 6;
        private int maxAttempts = 5;

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
    }

    public static class Auth {
        private int maxFailedLogins = 5;
        private Duration lockDuration = Duration.ofMinutes(15);
        private int passwordMinLength = 8;
        private int minAgeYears = 18;
        private final OAuth oauth = new OAuth();

        public OAuth getOauth() {
            return oauth;
        }

        public int getMaxFailedLogins() {
            return maxFailedLogins;
        }

        public void setMaxFailedLogins(int maxFailedLogins) {
            this.maxFailedLogins = maxFailedLogins;
        }

        public Duration getLockDuration() {
            return lockDuration;
        }

        public void setLockDuration(Duration lockDuration) {
            this.lockDuration = lockDuration;
        }

        public int getPasswordMinLength() {
            return passwordMinLength;
        }

        public void setPasswordMinLength(int passwordMinLength) {
            this.passwordMinLength = passwordMinLength;
        }

        public int getMinAgeYears() {
            return minAgeYears;
        }

        public void setMinAgeYears(int minAgeYears) {
            this.minAgeYears = minAgeYears;
        }
    }

    public static class OAuth {
        private String googleClientId = "";
        private String appleClientId = "";
        private String appleIssuer = "https://appleid.apple.com";

        public String getGoogleClientId() {
            return googleClientId;
        }

        public void setGoogleClientId(String googleClientId) {
            this.googleClientId = googleClientId;
        }

        public String getAppleClientId() {
            return appleClientId;
        }

        public void setAppleClientId(String appleClientId) {
            this.appleClientId = appleClientId;
        }

        public String getAppleIssuer() {
            return appleIssuer;
        }

        public void setAppleIssuer(String appleIssuer) {
            this.appleIssuer = appleIssuer;
        }
    }

    public static class Media {
        public enum Provider {
            S3,
            MINIO
        }

        private Provider provider = Provider.S3;
        private String bucket = "";
        private String region = "us-east-1";
        private String endpoint = "";
        private String accessKey = "";
        private String secretKey = "";
        private boolean pathStyleAccess = false;
        private Duration uploadUrlTtl = Duration.ofMinutes(10);
        private Duration downloadUrlTtl = Duration.ofHours(1);
        private long maxFileSizeBytes = 10L * 1024 * 1024;
        private List<String> allowedMimeTypes = List.of("image/jpeg", "image/png", "image/webp");

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public boolean isPathStyleAccess() {
            return pathStyleAccess;
        }

        public void setPathStyleAccess(boolean pathStyleAccess) {
            this.pathStyleAccess = pathStyleAccess;
        }

        public Duration getUploadUrlTtl() {
            return uploadUrlTtl;
        }

        public void setUploadUrlTtl(Duration uploadUrlTtl) {
            this.uploadUrlTtl = uploadUrlTtl;
        }

        public Duration getDownloadUrlTtl() {
            return downloadUrlTtl;
        }

        public void setDownloadUrlTtl(Duration downloadUrlTtl) {
            this.downloadUrlTtl = downloadUrlTtl;
        }

        public long getMaxFileSizeBytes() {
            return maxFileSizeBytes;
        }

        public void setMaxFileSizeBytes(long maxFileSizeBytes) {
            this.maxFileSizeBytes = maxFileSizeBytes;
        }

        public List<String> getAllowedMimeTypes() {
            return allowedMimeTypes;
        }

        public void setAllowedMimeTypes(List<String> allowedMimeTypes) {
            this.allowedMimeTypes = allowedMimeTypes;
        }
    }

    /** Blank keyId is how PaymentGatewayConfig decides to wire the stub gateway instead - see its class comment. */
    public static class Razorpay {
        private String keyId = "";
        private String keySecret = "";
        private String webhookSecret = "";

        public String getKeyId() {
            return keyId;
        }

        public void setKeyId(String keyId) {
            this.keyId = keyId;
        }

        public String getKeySecret() {
            return keySecret;
        }

        public void setKeySecret(String keySecret) {
            this.keySecret = keySecret;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }
    }
}
