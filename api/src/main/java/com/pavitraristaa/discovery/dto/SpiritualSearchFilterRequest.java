package com.pavitraristaa.discovery.dto;

/** All fields optional and text-based, matching docs/api/API_CONTRACT_V1.1.md section 4/7.1: spiritual discovery
 * filters are never required, and their absence must never be treated as a negative compatibility signal. */
public record SpiritualSearchFilterRequest(
        String spiritualCommunity,
        String spiritualInterest,
        String practice,
        String anySpiritualProfession
) {
}
