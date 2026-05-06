package com.adakadavra.dentis.clinic.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ClinicUserResponse {

    private final UUID id;
    private final String username;
    private final String email;
    private final String fullName;
    private final String role;
    private final boolean active;
}

