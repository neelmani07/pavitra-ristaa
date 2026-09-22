package com.pavitraristaa.profile.dto;

public record UpdateFamilyRequest(
        String familyType,
        String parentsStatus,
        Short siblingsCount,
        String familyDescription,
        String familyValues
) {
}
