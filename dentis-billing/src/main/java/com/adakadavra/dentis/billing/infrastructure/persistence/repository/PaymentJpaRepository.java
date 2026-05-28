package com.adakadavra.dentis.billing.infrastructure.persistence.repository;

import com.adakadavra.dentis.billing.infrastructure.persistence.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

    List<PaymentEntity> findByBudgetId(UUID budgetId);

    List<PaymentEntity> findByPatientId(UUID patientId);

    @Query("select coalesce(sum(p.amount), 0) from PaymentEntity p where p.budgetId = :budgetId")
    BigDecimal sumPaymentsByBudgetId(@Param("budgetId") UUID budgetId);
}

