package com.pavitraristaa.profile.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        String displayName,
        LocalDate dateOfBirth,
        Integer age,
        String gender,
        String headline,
        String aboutMe,
        String profileStatus,
        int profileCompletionPercent,
        Map<String, Object> location,
        List<String> relationshipModes,
        Map<String, Object> education,
        Map<String, Object> career,
        Map<String, Object> family,
        Map<String, Object> lifestyle,
        List<Map<String, Object>> languages,
        List<Map<String, Object>> interests,
        List<Map<String, Object>> hobbies,
        SpiritualProfileResponse spiritualProfile,
        List<ProfilePhotoResponse> photos,
        Map<String, Object> verification
) {
}
