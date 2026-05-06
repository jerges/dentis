package com.adakadavra.dentis.clinical.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@With
@AllArgsConstructor
public class TreatmentPlan {

    private final UUID id;
    private final UUID clinicalRecordId;
    private final UUID dentistId;
    private final String title;
    private final String description;
    private final TreatmentPlanStatus status;
    private final List<TreatmentProcedure> procedures;
    private final UUID budgetId;
    private final LocalDate startDate;
    private final LocalDate estimatedEndDate;
}
