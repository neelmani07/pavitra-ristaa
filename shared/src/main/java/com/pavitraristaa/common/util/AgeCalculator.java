package com.pavitraristaa.common.util;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;

public final class AgeCalculator {

    private AgeCalculator() {
    }

    public static Integer fromDateOfBirth(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return null;
        }
        return Period.between(dateOfBirth, LocalDate.now(ZoneOffset.UTC)).getYears();
    }
}
