package com.adakadavra.dentis.clinical.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.util.Map;
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
    /** Dental arch derived from tooth number; can be overridden for mixed-dentition displays. */
    private final DentalArch dentalArch;
    /** Primary/overall condition of the tooth. */
    private final ToothCondition condition;
    /** Simple set of surfaces involved in the primary condition. */
    private final Set<ToothSurface> affectedSurfaces;
    /** Fine-grained per-surface conditions (overrides affectedSurfaces when present). */
    private final Map<ToothSurface, ToothCondition> surfaceConditions;
    /** Space-closure triangle indicator shown when condition is ABSENT. Null for non-absent teeth. */
    private final SpaceStatus spaceStatus;
    private final String notes;
    private final Set<RootFinding> rootFindings;
    private final String rootNotes;
}
