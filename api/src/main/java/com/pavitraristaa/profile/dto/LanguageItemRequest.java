package com.pavitraristaa.profile.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LanguageItemRequest(
        Long languageId,
        String proficiency,
        @JsonProperty("isPrimary") Boolean primary
) {
}
