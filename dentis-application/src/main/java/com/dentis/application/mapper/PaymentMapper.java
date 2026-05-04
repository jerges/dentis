package com.dentis.application.mapper;

import com.dentis.application.dto.response.PaymentResponse;
import com.dentis.domain.entity.Payment;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    @Mapping(target = "budgetId", source = "budget.id")
    PaymentResponse toResponse(Payment payment);
}
