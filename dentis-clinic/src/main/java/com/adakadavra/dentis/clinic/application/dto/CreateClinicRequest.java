package com.adakadavra.dentis.clinic.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClinicRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 20)
    private String nif;

    @Size(max = 300)
    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String province;

    @Size(max = 10)
    private String zipCode;

    @Size(max = 20)
    private String phone;

    @Email
    @Size(max = 150)
    private String email;
}
