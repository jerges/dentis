package com.adakadavra.dentis.billing.infrastructure.persistence.repository;

import com.adakadavra.dentis.billing.domain.model.Payment;
import com.adakadavra.dentis.billing.domain.repository.PaymentRepository;
import com.adakadavra.dentis.billing.infrastructure.persistence.mapper.PaymentEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;
    private final PaymentEntityMapper entityMapper;

    @Override
    public Payment save(Payment payment) {
        return entityMapper.toDomain(jpaRepository.save(entityMapper.toEntity(payment)));
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return jpaRepository.findById(id).map(entityMapper::toDomain);
    }

    @Override
    public List<Payment> findByBudgetId(UUID budgetId) {
        return jpaRepository.findByBudgetId(budgetId)
                .stream()
                .map(entityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Payment> findByPatientId(UUID patientId) {
        return jpaRepository.findByPatientId(patientId)
                .stream()
                .map(entityMapper::toDomain)
                .toList();
    }

    @Override
    public BigDecimal sumPaymentsByBudgetId(UUID budgetId) {
        return jpaRepository.sumPaymentsByBudgetId(budgetId);
    }
}

