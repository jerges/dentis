package com.adakadavra.dentis.billing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@With
@AllArgsConstructor
public class BudgetItem {

    private final UUID id;
    private final UUID tariffId;
    private final String description;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal discountPercentage;
    private final boolean performed;
    private final LocalDateTime performedAt;
    private final ProcedurePaymentStatus paymentStatus;

    public BigDecimal totalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal discountAmount() {
        if (discountPercentage == null || discountPercentage.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalPrice()
                .multiply(discountPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal netPrice() {
        return totalPrice().subtract(discountAmount());
    }
}
