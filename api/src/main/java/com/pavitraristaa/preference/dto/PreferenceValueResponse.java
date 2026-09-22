package com.pavitraristaa.preference.dto;

public record PreferenceValueResponse(
        String preferenceType,
        Long masterValueId,
        String code,
        String name
) {
}
