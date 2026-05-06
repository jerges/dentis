package com.adakadavra.dentis.clinic.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateClinicRequest {

    @NotBlank
    @Size(max = 200)
    private final String name;

    @Size(max = 20)
    private final String nif;

    @Size(max = 300)
    private final String address;

    @Size(max = 100)
    private final String city;

    @Size(max = 100)
    private final String province;

    @Size(max = 10)
    private final String zipCode;

    @Size(max = 20)
    private final String phone;

    @Email
    @Size(max = 150)
    private final String email;
}

