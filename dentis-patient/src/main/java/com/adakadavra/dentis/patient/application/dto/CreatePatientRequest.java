package com.adakadavra.dentis.patient.application.dto;

import com.adakadavra.dentis.patient.domain.model.DocumentType;
import com.adakadavra.dentis.patient.domain.model.Gender;
import com.adakadavra.dentis.patient.domain.model.Sex;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePatientRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotNull
    private DocumentType documentType;

    @NotBlank
    private String idDocument;

    @NotNull
    @Past
    private LocalDate birthDate;

    @NotNull
    private Sex sex;

    @NotNull
    private Gender gender;

    private String socialName;

    @Valid
    @NotNull
    private ContactInfoDto contactInfo;

    @Valid
    private AddressDto address;

    @Valid
    private RepresentativeDto representative;
}
