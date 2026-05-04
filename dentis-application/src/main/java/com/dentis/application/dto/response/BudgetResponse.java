package com.dentis.application.dto.response;

import com.dentis.domain.enums.BudgetStatus;
import com.dentis.domain.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class BudgetResponse {
    private String id;
    private String patientId;
    private String patientName;
    private String dentistId;
    private String dentistName;
    private String treatmentPlanId;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private BudgetStatus status;
    private PaymentStatus paymentStatus;
    private String notes;
    private String currency;
    private BigDecimal totalAmount;
    private BigDecimal totalPaid;
    private BigDecimal balance;
    private List<BudgetItemResponse> items;
    private List<PaymentResponse> payments;
    private Instant createdAt;
}
