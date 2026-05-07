package com.adakadavra.dentis.billing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetSummary {

    private UUID budgetId;
    private BigDecimal grandTotal;
    private BigDecimal totalPaid;
    private BigDecimal balance;
    private PaymentSummaryStatus status;
}
