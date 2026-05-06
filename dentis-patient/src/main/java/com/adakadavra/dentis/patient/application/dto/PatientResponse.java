package com.adakadavra.dentis.patient.application.dto;

import com.adakadavra.dentis.patient.domain.model.Gender;
import com.adakadavra.dentis.patient.domain.model.Sex;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class PatientResponse {

    private final UUID id;
    private final String firstName;
    private final String lastName;
    private final String fullName;
    private final String idDocument;
    private final LocalDate birthDate;
    private final int age;
    private final Sex sex;
    private final Gender gender;
    private final String socialName;
    private final ContactInfoDto contactInfo;
    private final AddressDto address;
    private final RepresentativeDto representative;
    private final boolean active;
}
