package com.adakadavra.dentis.api.security.entity;

public enum UserRole {
    /** Platform-wide super administrator (e.g. jbello). Can manage all clinics. */
    SUPER_ADMIN,
    /** Clinic administrator. Can manage their own clinic's data and users. */
    ADMIN,
    /** Clinic staff member with standard access limited to their clinic. */
    USER
}
