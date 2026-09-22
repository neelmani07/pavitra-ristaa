package com.pavitraristaa.auth.service;

import com.pavitraristaa.auth.entity.LoginHistory;
import com.pavitraristaa.auth.entity.LoginType;
import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.repository.LoginHistoryRepository;
import com.pavitraristaa.common.util.ClientContext;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;

    public LoginHistoryService(LoginHistoryRepository loginHistoryRepository) {
        this.loginHistoryRepository = loginHistoryRepository;
    }

    @Transactional
    public void record(UserAccount user, LoginType loginType, boolean success, String failureReason, ClientContext context) {
        LoginHistory history = new LoginHistory();
        history.setUser(user);
        history.setLoginType(loginType);
        history.setSuccess(success);
        history.setFailureReason(failureReason);
        history.setLoginAt(Instant.now());
        if (context != null) {
            history.setIpAddress(sanitizeIp(context.ipAddress()));
            history.setUserAgent(truncate(context.userAgent(), 4000));
        }
        loginHistoryRepository.save(history);
    }

    private String sanitizeIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return null;
        }
        return ipAddress.length() > 64 ? ipAddress.substring(0, 64) : ipAddress;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
