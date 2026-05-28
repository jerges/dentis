package com.adakadavra.dentis.patient.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Representative {

    private final String fullName;
    private final String idDocument;
    private final String relationship;
    private final String phoneNumber;
}
