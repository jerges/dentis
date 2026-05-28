package com.adakadavra.dentis.clinical.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class Diagnosis {

    private final UUID id;
    private final UUID clinicalRecordId;
    private final String code;
    private final String description;
    private final LocalDate diagnosedAt;
    private final UUID dentistId;
}
