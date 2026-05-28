package com.adakadavra.dentis.clinical.domain.model;

public enum DentalArch {
    PERMANENT_UPPER,
    PERMANENT_LOWER,
    PRIMARY_UPPER,
    PRIMARY_LOWER;

    /** Derives arch from FDI tooth number (11-48 permanent, 51-85 primary). */
    public static DentalArch fromToothNumber(int toothNumber) {
        if (toothNumber >= 11 && toothNumber <= 28) return PERMANENT_UPPER;
        if (toothNumber >= 31 && toothNumber <= 48) return PERMANENT_LOWER;
        if (toothNumber >= 51 && toothNumber <= 65) return PRIMARY_UPPER;
        if (toothNumber >= 71 && toothNumber <= 85) return PRIMARY_LOWER;
        throw new IllegalArgumentException("Invalid FDI tooth number: " + toothNumber);
    }
}
