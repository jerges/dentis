package com.adakadavra.dentis.common.util;

import java.time.LocalDate;
import java.time.Period;

public final class DateUtils {

    private DateUtils() {}

    public static int calculateAge(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public static boolean isAdult(LocalDate birthDate) {
        return calculateAge(birthDate) >= 18;
    }
}
