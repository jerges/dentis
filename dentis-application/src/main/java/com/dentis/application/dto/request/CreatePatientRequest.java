package com.dentis.application.dto.request;

import com.dentis.domain.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CreatePatientRequest {

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @Size(max = 20)
    private String documentId;

    @NotNull
    @Past
    private LocalDate birthDate;

    @NotNull
    private Gender gender;

    @Size(max = 50)
    private String socialName;

    @Size(max = 20)
    private String phone;

    @Size(max = 20)
    private String altPhone;

    @Email
    @Size(max = 150)
    private String email;

    @Size(max = 255)
    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    private String representativeName;

    @Size(max = 20)
    private String representativePhone;

    @Size(max = 50)
    private String representativeRelationship;

    private String notes;
}
