package com.adakadavra.dentis.clinical.domain.model;

/** Estado del espacio cuando un diente está ausente (triángulo). */
public enum SpaceStatus {
    OPEN,             // espacio abierto (triángulo vacío)
    PARTIALLY_CLOSED, // espacio parcialmente cerrado (triángulo pintado a la mitad)
    CLOSED            // espacio cerrado (triángulo totalmente pintado)
}
