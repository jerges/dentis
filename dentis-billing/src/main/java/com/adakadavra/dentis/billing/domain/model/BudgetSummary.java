package com.adakadavra.dentis.billing.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class BudgetSummary {

    private final UUID budgetId;
    private final BigDecimal grandTotal;
    private final BigDecimal totalPaid;
    private final BigDecimal balance;
    private final PaymentSummaryStatus status;
}
