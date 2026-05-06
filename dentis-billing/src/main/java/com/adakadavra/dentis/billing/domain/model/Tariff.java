package com.adakadavra.dentis.billing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@With
@AllArgsConstructor
public class Tariff {

    private final UUID id;
    private final String code;
    private final String name;
    private final String description;
    private final TariffCategory category;
    private final BigDecimal basePrice;
    private final boolean discountAllowed;
    private final boolean active;
}
