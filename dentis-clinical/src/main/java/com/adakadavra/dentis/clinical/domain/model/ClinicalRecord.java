package com.adakadavra.dentis.clinical.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@With
@AllArgsConstructor
public class ClinicalRecord {

    private final UUID id;
    private final UUID patientId;
    private final DentitionType dentitionType;
    private final List<OdontogramTooth> odontogram;
    private final List<ClinicalEvolution> evolutions;
    private final List<Diagnosis> diagnoses;
    private final List<TreatmentPlan> treatmentPlans;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
