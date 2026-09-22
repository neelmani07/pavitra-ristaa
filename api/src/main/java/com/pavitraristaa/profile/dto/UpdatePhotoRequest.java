package com.pavitraristaa.profile.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdatePhotoRequest(
        String photoType,
        Integer displayOrder,
        @JsonProperty("isPrimary") Boolean primary,
        String visibility
) {
}
