package com.adakadavra.dentis.patient.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@With
@AllArgsConstructor
public class Patient {

    private final UUID id;
    private final String firstName;
    private final String lastName;
    private final DocumentType documentType;
    private final String idDocument;
    private final LocalDate birthDate;
    private final Sex sex;
    private final Gender gender;
    private final String socialName;
    private final ContactInfo contactInfo;
    private final Address address;
    private final Representative representative;
    private final boolean active;
    /** Clinic this patient belongs to. */
    private final UUID clinicId;

    public String fullName() {
        return firstName + " " + lastName;
    }

    public boolean isMinor() {
        return birthDate != null && LocalDate.now().getYear() - birthDate.getYear() < 18;
    }
}
