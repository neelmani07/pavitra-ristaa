package com.pavitraristaa.preference.entity;

/**
 * Multi-select partner preference types and the master-data category each one draws its values from.
 */
public enum PreferenceType {
    RELIGION("RELIGION"),
    LANGUAGE("LANGUAGE"),
    EDUCATION("EDUCATION_LEVEL"),
    OCCUPATION("OCCUPATION"),
    INDUSTRY("INDUSTRY"),
    DIET("DIET"),
    SMOKING("SMOKING"),
    DRINKING("DRINKING"),
    EXERCISE_FREQUENCY("EXERCISE_FREQUENCY"),
    COUNTRY("COUNTRY"),
    STATE("STATE"),
    CITY("CITY"),
    INTEREST("INTEREST"),
    HOBBY("HOBBY");

    private final String categoryCode;

    PreferenceType(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String categoryCode() {
        return categoryCode;
    }
}
