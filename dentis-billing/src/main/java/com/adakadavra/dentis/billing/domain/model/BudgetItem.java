package com.adakadavra.dentis.billing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@With
@NoArgsConstructor
@AllArgsConstructor
public class BudgetItem {

    private UUID id;
    private UUID tariffId;
    private String description;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountPercentage;
    private Boolean performed;
    private LocalDateTime performedAt;
    private ProcedurePaymentStatus paymentStatus;

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
