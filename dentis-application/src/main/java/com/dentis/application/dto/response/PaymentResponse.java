package com.dentis.application.dto.response;

import com.dentis.domain.enums.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private String id;
    private String budgetId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private LocalDateTime paymentDate;
    private String reference;
    private String invoiceNumber;
    private String currency;
    private String notes;
    private Instant createdAt;
}
