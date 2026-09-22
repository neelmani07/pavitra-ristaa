package com.pavitraristaa.profile.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record CreatePhotoRequest(
        UUID mediaFileId,
        String photoType,
        Integer displayOrder,
        @JsonProperty("isPrimary") Boolean primary,
        String visibility
) {
}
