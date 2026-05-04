package com.dentis.application.dto.request;

import com.dentis.domain.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CreatePaymentRequest {

    @NotBlank
    private String budgetId;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotNull
    private PaymentMethod paymentMethod;

    @NotNull
    private LocalDateTime paymentDate;

    private String reference;

    private String invoiceNumber;

    private String currency;

    private String notes;
}
