package com.dentis.application.dto.response;

import com.dentis.domain.enums.Gender;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class PatientResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String documentId;
    private LocalDate birthDate;
    private int age;
    private Gender gender;
    private String socialName;
    private String phone;
    private String altPhone;
    private String email;
    private String address;
    private String city;
    private String state;
    private String representativeName;
    private String representativePhone;
    private String representativeRelationship;
    private String notes;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
