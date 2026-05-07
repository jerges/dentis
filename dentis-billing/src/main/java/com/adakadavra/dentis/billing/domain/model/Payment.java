package com.adakadavra.dentis.billing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    private UUID id;
    private UUID patientId;
    private UUID budgetId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String invoiceReference;
    private String notes;
    private LocalDateTime paidAt;
}
