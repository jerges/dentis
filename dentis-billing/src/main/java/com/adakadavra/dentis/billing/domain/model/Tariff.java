package com.adakadavra.dentis.billing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@With
@NoArgsConstructor
@AllArgsConstructor
public class Tariff {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private TariffCategory category;
    private BigDecimal basePrice;
    private boolean discountAllowed;
    private boolean active;
}
