package com.pavitraristaa.auth.service;

import com.pavitraristaa.auth.entity.AccountStatus;
import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class AccountStateGuard {

    public void assertCanAuthenticate(UserAccount user) {
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new ApiException(ErrorCode.TOO_MANY_ATTEMPTS, "Account is temporarily locked");
        }
        switch (user.getAccountStatus()) {
            case PENDING_VERIFICATION -> throw new ApiException(ErrorCode.ACCOUNT_NOT_VERIFIED, "Account is not verified");
            case SUSPENDED -> throw new ApiException(ErrorCode.ACCOUNT_SUSPENDED, "Account is suspended");
            case BLOCKED -> throw new ApiException(ErrorCode.FORBIDDEN, "Account is blocked");
            case DELETED -> throw new ApiException(ErrorCode.ACCOUNT_DELETED, "Account has been deleted");
            case ACTIVE -> {
                if (user.isDeleted()) {
                    throw new ApiException(ErrorCode.ACCOUNT_DELETED, "Account has been deleted");
                }
            }
        }
    }

    public void assertUsableSession(UserAccount user) {
        if (user.getAccountStatus() == AccountStatus.DELETED || user.isDeleted()) {
            throw new ApiException(ErrorCode.ACCOUNT_DELETED, "Account has been deleted");
        }
        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new ApiException(ErrorCode.ACCOUNT_SUSPENDED, "Account is suspended");
        }
        if (user.getAccountStatus() == AccountStatus.BLOCKED) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Account is blocked");
        }
    }
}
