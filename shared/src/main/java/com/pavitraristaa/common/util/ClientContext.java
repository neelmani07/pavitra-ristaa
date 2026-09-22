package com.pavitraristaa.common.util;

import jakarta.servlet.http.HttpServletRequest;

public record ClientContext(String ipAddress, String userAgent) {

    public static ClientContext from(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip;
        if (forwarded != null && !forwarded.isBlank()) {
            ip = forwarded.split(",")[0].trim();
        } else {
            ip = request.getRemoteAddr();
        }
        return new ClientContext(ip, request.getHeader("User-Agent"));
    }
}
