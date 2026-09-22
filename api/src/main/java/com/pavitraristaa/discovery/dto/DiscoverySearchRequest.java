package com.pavitraristaa.discovery.dto;

import java.util.List;

public record DiscoverySearchRequest(
        String query,
        List<String> relationshipModes,
        Integer minAge,
        Integer maxAge,
        Long countryId,
        Long stateId,
        Long cityId,
        SpiritualSearchFilterRequest spiritual,
        Integer page,
        Integer size
) {
}
