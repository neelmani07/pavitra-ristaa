package com.pavitraristaa.discovery.dto;

import com.pavitraristaa.profile.dto.UserSummaryResponse;
import java.util.List;

public record CollectionDetailResponse(
        String code,
        String name,
        String description,
        List<UserSummaryResponse> members
) {
}
