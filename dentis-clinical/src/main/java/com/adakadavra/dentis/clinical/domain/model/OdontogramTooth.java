package com.adakadavra.dentis.clinical.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.util.Set;
import java.util.UUID;

/**
 * Represents a single tooth in the odontogram using the FDI World Dental Federation numbering.
 * Tooth numbers range from 11-48 (permanent) and 51-85 (primary).
 */
@Getter
@Builder
@With
@AllArgsConstructor
public class OdontogramTooth {

    private final UUID id;
    private final int toothNumber;
    private final ToothCondition condition;
    private final Set<ToothSurface> affectedSurfaces;
    private final String notes;
}
