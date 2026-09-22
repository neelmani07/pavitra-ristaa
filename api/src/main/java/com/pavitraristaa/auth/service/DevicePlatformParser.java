package com.pavitraristaa.auth.service;

import com.pavitraristaa.auth.entity.DevicePlatform;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import java.util.Map;

public final class DevicePlatformParser {

    private DevicePlatformParser() {
    }

    public static DevicePlatform parse(String platform) {
        if (platform == null || platform.isBlank()) {
            return null;
        }
        try {
            return DevicePlatform.valueOf(platform.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Unsupported device platform", Map.of("platform", platform));
        }
    }
}
