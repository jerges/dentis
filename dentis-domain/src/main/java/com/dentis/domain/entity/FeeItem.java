package com.dentis.domain.entity;

import com.dentis.domain.enums.FeeCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "fee_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeItem extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FeeCategory category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal maxDiscountPercentage = BigDecimal.ZERO;

    private boolean discountAllowed = true;
    private boolean active = true;

    @Column(length = 50)
    private String code;

    @Column(length = 50)
    private String currency;

    public BigDecimal getPriceWithDiscount(BigDecimal discountPercentage) {
        if (!discountAllowed || discountPercentage == null || discountPercentage.compareTo(BigDecimal.ZERO) == 0) {
            return basePrice;
        }
        BigDecimal effectiveDiscount = discountPercentage.min(maxDiscountPercentage);
        BigDecimal discountAmount = basePrice.multiply(effectiveDiscount).divide(BigDecimal.valueOf(100));
        return basePrice.subtract(discountAmount);
    }
}
