package com.adakadavra.dentis.patient.application.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {

    private String street;
    private String city;
    private String state;
    private String zipCode;
}
