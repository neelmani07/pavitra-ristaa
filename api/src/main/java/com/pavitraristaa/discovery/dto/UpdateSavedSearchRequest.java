package com.pavitraristaa.discovery.dto;

import java.util.Map;

/**
 * The contract leaves this body fully freeform (additionalProperties: true, no fixed schema). Interpreted here
 * as a partial update: every field is optional and only a field that's actually present changes anything.
 */
public record UpdateSavedSearchRequest(
        String name,
        Map<String, Object> criteria,
        Boolean isDefault
) {
}
