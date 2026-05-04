package com.dentis.application.mapper;

import com.dentis.application.dto.response.BudgetItemResponse;
import com.dentis.application.dto.response.BudgetResponse;
import com.dentis.domain.entity.Budget;
import com.dentis.domain.entity.BudgetItem;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {PaymentMapper.class})
public interface BudgetMapper {

    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientName", expression = "java(budget.getPatient().getFullName())")
    @Mapping(target = "dentistId", source = "dentist.id")
    @Mapping(target = "dentistName", expression = "java(budget.getDentist() != null ? budget.getDentist().getFullName() : null)")
    @Mapping(target = "treatmentPlanId", source = "treatmentPlan.id")
    @Mapping(target = "totalAmount", expression = "java(budget.getTotalAmount())")
    @Mapping(target = "totalPaid", expression = "java(budget.getTotalPaid())")
    @Mapping(target = "balance", expression = "java(budget.getBalance())")
    BudgetResponse toResponse(Budget budget);

    @Mapping(target = "feeItemId", source = "feeItem.id")
    @Mapping(target = "feeItemName", source = "feeItem.name")
    @Mapping(target = "totalPrice", expression = "java(item.getTotalPrice())")
    BudgetItemResponse toItemResponse(BudgetItem item);
}
