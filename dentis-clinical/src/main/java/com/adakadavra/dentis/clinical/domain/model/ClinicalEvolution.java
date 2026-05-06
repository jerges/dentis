package com.adakadavra.dentis.clinical.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ClinicalEvolution {

    private final UUID id;
    private final UUID clinicalRecordId;
    private final UUID dentistId;
    private final String description;
    private final String findings;
    private final String treatment;
    private final LocalDateTime recordedAt;
}
