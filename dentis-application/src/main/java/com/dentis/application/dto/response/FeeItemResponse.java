package com.dentis.application.dto.response;

import com.dentis.domain.enums.FeeCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class FeeItemResponse {
    private String id;
    private String name;
    private String description;
    private FeeCategory category;
    private BigDecimal basePrice;
    private BigDecimal maxDiscountPercentage;
    private boolean discountAllowed;
    private String code;
    private String currency;
    private boolean active;
}
