package com.adakadavra.dentis.patient.domain.model;

public enum DocumentType {
    /** Venezuelan national ID card (cédula venezolana, prefix V-). */
    NATIONAL_ID,
    /** Foreign resident ID card (cédula extranjero, prefix E-). */
    FOREIGN_ID,
    /** Passport — alphanumeric, issued by any country. */
    PASSPORT,
    /** Tax registration number (RIF — Registro de Información Fiscal). */
    RIF
}
