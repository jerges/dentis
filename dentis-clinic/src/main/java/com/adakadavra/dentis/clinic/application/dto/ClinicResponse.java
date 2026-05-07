package com.adakadavra.dentis.clinic.application.dto;

import com.adakadavra.dentis.clinic.domain.model.Clinic;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicResponse {

    private UUID id;
    private String name;
    private String nif;
    private String address;
    private String city;
    private String province;
    private String zipCode;
    private String phone;
    private String email;
    private boolean active;
    private LocalDateTime createdAt;

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
