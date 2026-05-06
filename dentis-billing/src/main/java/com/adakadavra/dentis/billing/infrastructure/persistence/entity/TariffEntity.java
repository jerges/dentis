package com.adakadavra.dentis.billing.infrastructure.persistence.entity;

import com.adakadavra.dentis.billing.domain.model.TariffCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tariffs", indexes = {
        @Index(name = "idx_tariff_category", columnList = "category"),
        @Index(name = "idx_tariff_code", columnList = "code")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TariffEntity {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private TariffCategory category;

    @Column(name = "base_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "discount_allowed", nullable = false)
    @Builder.Default
    private boolean discountAllowed = true;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}

