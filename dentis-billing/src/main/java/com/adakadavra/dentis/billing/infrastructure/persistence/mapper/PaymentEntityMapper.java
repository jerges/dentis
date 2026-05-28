package com.adakadavra.dentis.billing.infrastructure.persistence.mapper;

import com.adakadavra.dentis.billing.domain.model.Payment;
import com.adakadavra.dentis.billing.infrastructure.persistence.entity.PaymentEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentEntityMapper {

    public PaymentEntity toEntity(Payment payment) {
        if (payment == null) {
            return null;
        }
        return PaymentEntity.builder()
                .id(payment.getId())
                .patientId(payment.getPatientId())
                .budgetId(payment.getBudgetId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .invoiceReference(payment.getInvoiceReference())
                .notes(payment.getNotes())
                .paidAt(payment.getPaidAt())
                .build();
    }

    public Payment toDomain(PaymentEntity entity) {
        if (entity == null) {
            return null;
        }
        return Payment.builder()
                .id(entity.getId())
                .patientId(entity.getPatientId())
                .budgetId(entity.getBudgetId())
                .amount(entity.getAmount())
                .paymentMethod(entity.getPaymentMethod())
                .invoiceReference(entity.getInvoiceReference())
                .notes(entity.getNotes())
                .paidAt(entity.getPaidAt())
                .build();
    }
}

