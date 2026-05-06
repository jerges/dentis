package com.adakadavra.dentis.clinic.application.dto;

import com.adakadavra.dentis.clinic.domain.model.Clinic;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ClinicResponse {

    private final UUID id;
    private final String name;
    private final String nif;
    private final String address;
    private final String city;
    private final String province;
    private final String zipCode;
    private final String phone;
    private final String email;
    private final boolean active;
    private final LocalDateTime createdAt;

    public static ClinicResponse from(Clinic clinic) {
        return ClinicResponse.builder()
                .id(clinic.getId())
                .name(clinic.getName())
                .nif(clinic.getNif())
                .address(clinic.getAddress())
                .city(clinic.getCity())
                .province(clinic.getProvince())
                .zipCode(clinic.getZipCode())
                .phone(clinic.getPhone())
                .email(clinic.getEmail())
                .active(clinic.isActive())
                .createdAt(clinic.getCreatedAt())
                .build();
    }
}

