package com.pavitraristaa.profile.dto;

import java.util.List;

public record ProfileCompletionResponse(
        int percent,
        List<String> missingSections
) {
}
