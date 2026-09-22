package com.pavitraristaa.profile.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record ProfilePhotoResponse(
        UUID id,
        String url,
        String photoType,
        int displayOrder,
        @JsonProperty("isPrimary") boolean isPrimary,
        String visibility,
        String approvalStatus
) {
}
