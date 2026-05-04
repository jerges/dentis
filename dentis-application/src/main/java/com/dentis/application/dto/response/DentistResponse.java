package com.dentis.application.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DentistResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String licenseNumber;
    private String specialty;
    private String colorCode;
    private boolean active;
    private Instant createdAt;
}
