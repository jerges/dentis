package com.adakadavra.dentis.patient.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContactInfo {

    private final String email;
    private final String phoneNumber;
    private final String alternativePhone;
}
