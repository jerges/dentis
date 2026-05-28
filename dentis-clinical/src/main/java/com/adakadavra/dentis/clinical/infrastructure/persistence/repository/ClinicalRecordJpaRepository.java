package com.adakadavra.dentis.clinical.infrastructure.persistence.repository;

import com.adakadavra.dentis.clinical.infrastructure.persistence.entity.ClinicalRecordEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClinicalRecordJpaRepository extends JpaRepository<ClinicalRecordEntity, UUID> {

    @EntityGraph(attributePaths = {"odontogram", "evolutions", "diagnoses", "treatmentPlans", "treatmentPlans.procedures"})
    Optional<ClinicalRecordEntity> findByPatientId(UUID patientId);

    boolean existsByPatientId(UUID patientId);

    @Override
    @EntityGraph(attributePaths = {"odontogram", "evolutions", "diagnoses", "treatmentPlans", "treatmentPlans.procedures"})
    Optional<ClinicalRecordEntity> findById(UUID id);
}

