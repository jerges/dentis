package com.dentis.domain.entity;

import com.dentis.domain.enums.TreatmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "treatment_procedures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreatmentProcedure extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "treatment_plan_id", nullable = false)
    private TreatmentPlan treatmentPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_item_id")
    private FeeItem feeItem;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal agreedPrice;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(length = 10)
    private String toothNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TreatmentStatus status = TreatmentStatus.PENDING;

    private LocalDate performedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_id")
    private Dentist performedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public BigDecimal getTotalPrice() {
        return agreedPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
