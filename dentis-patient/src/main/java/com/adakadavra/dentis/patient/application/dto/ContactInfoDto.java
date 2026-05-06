package com.adakadavra.dentis.patient.application.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactInfoDto {

    @Email
    private String email;
    private String phoneNumber;
    private String alternativePhone;
}
