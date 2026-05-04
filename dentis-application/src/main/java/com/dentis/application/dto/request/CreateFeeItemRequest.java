package com.dentis.application.dto.request;

import com.dentis.domain.enums.FeeCategory;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CreateFeeItemRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    private String description;

    @NotNull
    private FeeCategory category;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal basePrice;

    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal maxDiscountPercentage;

    private boolean discountAllowed = true;

    @Size(max = 50)
    private String code;

    @Size(max = 50)
    private String currency;
}
