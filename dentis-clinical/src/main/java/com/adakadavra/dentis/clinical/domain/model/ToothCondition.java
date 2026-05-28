package com.adakadavra.dentis.clinical.domain.model;

public enum ToothCondition {
    HEALTHY,
    ABSENT,
    CARIES,
    RESTORED,
    CROWNED,
    IMPLANT,
    ROOT_CANAL,
    FRACTURE,
    MALFORMATION,
    /** Radicular rest — tooth crown is lost, only the root remains. */
    RADICULAR_REST,
    /** Defective filling — existing restoration is compromised or failed. */
    DEFECTIVE_FILLING,
    ERUPTING,
    IMPACTED
}
