package com.adakadavra.dentis.clinical.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@With
@AllArgsConstructor
public class TreatmentProcedure {

    private final UUID id;
    private final UUID treatmentPlanId;
    private final String description;
    private final Integer toothNumber;
    private final boolean performed;
    private final LocalDateTime performedAt;
    private final UUID performedByDentistId;
    private final UUID budgetItemId;
}
