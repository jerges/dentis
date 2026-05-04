package com.dentis.application.dto.response;

import com.dentis.domain.enums.Gender;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PatientSummaryResponse {
    private String id;
    private String fullName;
    private String documentId;
    private LocalDate birthDate;
    private int age;
    private Gender gender;
    private String phone;
    private String email;
    private boolean active;
}
