package com.adakadavra.dentis.billing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@With
@AllArgsConstructor
public class Budget {

    private final UUID id;
    private final UUID patientId;
    private final UUID treatmentPlanId;
    private final UUID dentistId;
    private final List<BudgetItem> items;
    private final BudgetStatus status;
    private final String notes;
    private final LocalDateTime createdAt;
    private final LocalDateTime approvedAt;

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
