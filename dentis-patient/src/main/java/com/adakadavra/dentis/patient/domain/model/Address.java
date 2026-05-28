package com.adakadavra.dentis.patient.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Address {

    private final String street;
    private final String city;
    private final String state;
    private final String zipCode;
}
