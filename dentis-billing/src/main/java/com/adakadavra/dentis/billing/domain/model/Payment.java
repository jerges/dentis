package com.adakadavra.dentis.billing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class Payment {

    private final UUID id;
    private final UUID patientId;
    private final UUID budgetId;
    private final BigDecimal amount;
    private final PaymentMethod paymentMethod;
    private final String invoiceReference;
    private final String notes;
    private final LocalDateTime paidAt;
}
