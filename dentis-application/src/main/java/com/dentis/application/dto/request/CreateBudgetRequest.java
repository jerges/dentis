package com.dentis.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class CreateBudgetRequest {

    @NotBlank
    private String patientId;

    private String dentistId;

    private String treatmentPlanId;

    @NotNull
    private LocalDate issueDate;

    @NotNull
    private LocalDate expiryDate;

    private String notes;

    @Size(max = 50)
    private String currency;

    @NotEmpty
    @Valid
    private List<BudgetItemRequest> items;

    @Data
    @Builder
    public static class BudgetItemRequest {

        private String feeItemId;

        @NotBlank
        private String description;

        @NotNull
        @DecimalMin("0.00")
        private BigDecimal unitPrice;

        @DecimalMin("0.00")
        @DecimalMax("100.00")
        private BigDecimal discountPercentage;

        @NotNull
        @Min(1)
        private Integer quantity;

        @Size(max = 10)
        private String toothNumber;
    }
}
