package com.adakadavra.dentis.patient.application.dto;

import com.adakadavra.dentis.patient.domain.model.Gender;
import com.adakadavra.dentis.patient.domain.model.Sex;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePatientRequest {

    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private Sex sex;
    private Gender gender;
    private String socialName;

    @Valid
    private ContactInfoDto contactInfo;

    @Valid
    private AddressDto address;

    @Valid
    private RepresentativeDto representative;
}
