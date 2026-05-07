package com.adakadavra.dentis.billing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@With
@NoArgsConstructor
@AllArgsConstructor
public class Budget {

    private UUID id;
    private UUID patientId;
    private UUID treatmentPlanId;
    private UUID dentistId;
    private List<BudgetItem> items;
    private BudgetStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;

    public BigDecimal subtotal() {
        return items.stream()
                .map(BudgetItem::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalDiscount() {
        return items.stream()
                .map(BudgetItem::discountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal grandTotal() {
        return subtotal().subtract(totalDiscount());
    }
}
