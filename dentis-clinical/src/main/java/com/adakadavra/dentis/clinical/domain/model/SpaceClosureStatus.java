package com.adakadavra.dentis.clinical.domain.model;

/**
 * Represents the space remaining after a tooth is absent.
 * Rendered as a triangle indicator in the odontogram UI:
 * open triangle = OPEN, half-filled = PARTIALLY_CLOSED, filled = CLOSED.
 */
public enum SpaceClosureStatus {
    OPEN,
    PARTIALLY_CLOSED,
    CLOSED
}
