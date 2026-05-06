package com.adakadavra.dentis.billing.infrastructure.persistence.repository;

import com.adakadavra.dentis.billing.domain.model.Budget;
import com.adakadavra.dentis.billing.domain.repository.BudgetRepository;
import com.adakadavra.dentis.billing.infrastructure.persistence.mapper.BudgetEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BudgetRepositoryAdapter implements BudgetRepository {

    private final BudgetJpaRepository jpaRepository;
    private final BudgetEntityMapper entityMapper;

    @Override
    public Budget save(Budget budget) {
        return entityMapper.toDomain(jpaRepository.save(entityMapper.toEntity(budget)));
    }

    @Override
    public Optional<Budget> findById(UUID id) {
        return jpaRepository.findById(id).map(entityMapper::toDomain);
    }

    @Override
    public Page<Budget> findByPatientId(UUID patientId, Pageable pageable) {
        return jpaRepository.findByPatientId(patientId, pageable).map(entityMapper::toDomain);
    }

    @Override
    public List<Budget> findByTreatmentPlanId(UUID treatmentPlanId) {
        return jpaRepository.findByTreatmentPlanId(treatmentPlanId)
                .stream()
                .map(entityMapper::toDomain)
                .toList();
    }
}

