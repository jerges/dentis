package com.adakadavra.dentis.billing.infrastructure.persistence.repository;

import com.adakadavra.dentis.billing.infrastructure.persistence.entity.BudgetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BudgetJpaRepository extends JpaRepository<BudgetEntity, UUID> {

    @EntityGraph(attributePaths = "items")
    Page<BudgetEntity> findByPatientId(UUID patientId, Pageable pageable);

    @EntityGraph(attributePaths = "items")
    List<BudgetEntity> findByTreatmentPlanId(UUID treatmentPlanId);

    @Override
    @EntityGraph(attributePaths = "items")
    java.util.Optional<BudgetEntity> findById(UUID id);
}

