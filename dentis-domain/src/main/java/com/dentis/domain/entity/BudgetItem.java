package com.dentis.domain.entity;

import com.dentis.domain.enums.TreatmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "budget_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_item_id")
    private FeeItem feeItem;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(length = 10)
    private String toothNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TreatmentStatus procedureStatus = TreatmentStatus.PENDING;

    private LocalDate performedDate;

    @Column(columnDefinition = "TEXT")
    private String invoiceReference;

    public BigDecimal getTotalPrice() {
        BigDecimal discounted = unitPrice.subtract(
                unitPrice.multiply(discountPercentage).divide(BigDecimal.valueOf(100))
        );
        return discounted.multiply(BigDecimal.valueOf(quantity));
    }
}
