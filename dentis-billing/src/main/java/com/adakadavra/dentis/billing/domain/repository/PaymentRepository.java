package com.adakadavra.dentis.billing.domain.repository;

import com.adakadavra.dentis.billing.domain.model.Payment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);

    List<Payment> findByBudgetId(UUID budgetId);

    List<Payment> findByPatientId(UUID patientId);

    BigDecimal sumPaymentsByBudgetId(UUID budgetId);
}
