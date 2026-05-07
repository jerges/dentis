package com.adakadavra.dentis.patient.application.dto;

import com.adakadavra.dentis.patient.domain.model.Gender;
import com.adakadavra.dentis.patient.domain.model.Sex;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String idDocument;
    private LocalDate birthDate;
    private int age;
    private Sex sex;
    private Gender gender;
    private String socialName;
    private ContactInfoDto contactInfo;
    private AddressDto address;
    private RepresentativeDto representative;
    private boolean active;
}
