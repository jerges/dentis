package com.adakadavra.dentis.patient.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Representative {

    /** When true the patient has no representative; all other fields are ignored. */
    private final boolean notApplicable;
    private final String fullName;
    private final String idDocument;
    private final String relationship;
    private final String phoneNumber;
}
