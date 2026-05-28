package com.adakadavra.dentis.billing.domain.repository;

import com.adakadavra.dentis.billing.domain.model.Budget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository {

    Budget save(Budget budget);

    Optional<Budget> findById(UUID id);

    Page<Budget> findByPatientId(UUID patientId, Pageable pageable);

    List<Budget> findByTreatmentPlanId(UUID treatmentPlanId);
}
