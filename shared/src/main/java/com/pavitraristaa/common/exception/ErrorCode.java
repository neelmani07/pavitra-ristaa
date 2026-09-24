package com.pavitraristaa.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT),
    ACCOUNT_NOT_VERIFIED(HttpStatus.FORBIDDEN),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN),
    ACCOUNT_DELETED(HttpStatus.FORBIDDEN),
    OTP_INVALID(HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(HttpStatus.BAD_REQUEST),
    TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED),
    SESSION_REVOKED(HttpStatus.UNAUTHORIZED),
    PROFILE_NOT_ACTIVE(HttpStatus.FORBIDDEN),
    CANNOT_INTERACT_WITH_SELF(HttpStatus.BAD_REQUEST),
    USER_BLOCKED(HttpStatus.FORBIDDEN),
    ALREADY_INTERESTED(HttpStatus.CONFLICT),
    INTEREST_NOT_ACTIONABLE(HttpStatus.CONFLICT),
    MATCH_NOT_FOUND(HttpStatus.NOT_FOUND),
    CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND),
    MEDIA_NOT_FOUND(HttpStatus.NOT_FOUND),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST),
    SUBSCRIPTION_NOT_ACTIVE(HttpStatus.FORBIDDEN),
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND),
    VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND),
    SUPPORT_TICKET_NOT_FOUND(HttpStatus.NOT_FOUND),
    MODERATION_NOT_FOUND(HttpStatus.NOT_FOUND),
    SETTING_NOT_FOUND(HttpStatus.NOT_FOUND),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
