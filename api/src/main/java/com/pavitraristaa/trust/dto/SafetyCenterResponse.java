package com.pavitraristaa.trust.dto;

import java.util.List;

public record SafetyCenterResponse(List<SafetyTip> tips) {

    public record SafetyTip(String title, String body) {
    }
}
