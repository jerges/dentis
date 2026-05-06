package com.adakadavra.dentis.clinic.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@With
@AllArgsConstructor
public class Clinic {

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
    private final LocalDateTime updatedAt;
}

