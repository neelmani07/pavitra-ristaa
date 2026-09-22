package com.pavitraristaa.profile.dto;

public record UpdateEducationRequest(
        Long educationLevelId,
        String institutionName,
        String fieldOfStudy,
        Short graduationYear,
        String additionalDetails
) {
}
