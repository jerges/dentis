package com.adakadavra.dentis.clinic.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicUserResponse {

    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private boolean active;
}
