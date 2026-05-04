package com.dentis.application.dto.response;

import com.dentis.domain.enums.TreatmentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class BudgetItemResponse {
    private String id;
    private String feeItemId;
    private String feeItemName;
    private String description;
    private BigDecimal unitPrice;
    private BigDecimal discountPercentage;
    private Integer quantity;
    private BigDecimal totalPrice;
    private String toothNumber;
    private TreatmentStatus procedureStatus;
    private LocalDate performedDate;
    private String invoiceReference;
}
