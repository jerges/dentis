package com.adakadavra.dentis.billing.infrastructure.persistence.mapper;

import com.adakadavra.dentis.billing.domain.model.Budget;
import com.adakadavra.dentis.billing.domain.model.BudgetItem;
import com.adakadavra.dentis.billing.domain.model.ProcedurePaymentStatus;
import com.adakadavra.dentis.billing.infrastructure.persistence.entity.BudgetEntity;
import com.adakadavra.dentis.billing.infrastructure.persistence.entity.BudgetItemEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class BudgetEntityMapper {

    public BudgetEntity toEntity(Budget budget) {
        if (budget == null) {
            return null;
        }

        BudgetEntity entity = BudgetEntity.builder()
                .id(budget.getId())
                .patientId(budget.getPatientId())
                .treatmentPlanId(budget.getTreatmentPlanId())
                .dentistId(budget.getDentistId())
                .status(budget.getStatus())
                .notes(budget.getNotes())
                .createdAt(budget.getCreatedAt())
                .approvedAt(budget.getApprovedAt())
                .items(new ArrayList<>())
                .build();

        if (budget.getItems() != null) {
            for (BudgetItem item : budget.getItems()) {
                BudgetItemEntity itemEntity = BudgetItemEntity.builder()
                        .id(item.getId())
                        .budget(entity)
                        .tariffId(item.getTariffId())
                        .description(item.getDescription())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .discountPercentage(item.getDiscountPercentage() == null ? BigDecimal.ZERO : item.getDiscountPercentage())
                        .performed(item.isPerformed())
                        .performedAt(item.getPerformedAt())
                        .paymentStatus(item.getPaymentStatus() == null ? ProcedurePaymentStatus.PENDING : item.getPaymentStatus())
                        .build();
                entity.getItems().add(itemEntity);
            }
        }

        return entity;
    }

    public Budget toDomain(BudgetEntity entity) {
        if (entity == null) {
            return null;
        }

        List<BudgetItem> items = new ArrayList<>();
        if (entity.getItems() != null) {
            for (BudgetItemEntity itemEntity : entity.getItems()) {
                items.add(BudgetItem.builder()
                        .id(itemEntity.getId())
                        .tariffId(itemEntity.getTariffId())
                        .description(itemEntity.getDescription())
                        .quantity(itemEntity.getQuantity())
                        .unitPrice(itemEntity.getUnitPrice())
                        .discountPercentage(itemEntity.getDiscountPercentage())
                        .performed(itemEntity.isPerformed())
                        .performedAt(itemEntity.getPerformedAt())
                        .paymentStatus(itemEntity.getPaymentStatus())
                        .build());
            }
        }

        return Budget.builder()
                .id(entity.getId())
                .patientId(entity.getPatientId())
                .treatmentPlanId(entity.getTreatmentPlanId())
                .dentistId(entity.getDentistId())
                .items(items)
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .approvedAt(entity.getApprovedAt())
                .build();
    }
}

