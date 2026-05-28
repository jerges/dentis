package com.adakadavra.dentis.clinical.infrastructure.persistence.repository;

import com.adakadavra.dentis.clinical.domain.model.ClinicalRecord;
import com.adakadavra.dentis.clinical.domain.repository.ClinicalRecordRepository;
import com.adakadavra.dentis.clinical.infrastructure.persistence.mapper.ClinicalRecordEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ClinicalRecordRepositoryAdapter implements ClinicalRecordRepository {

    private final ClinicalRecordJpaRepository jpaRepository;
    private final ClinicalRecordEntityMapper entityMapper;

    @Override
    public ClinicalRecord save(ClinicalRecord clinicalRecord) {
        return entityMapper.toDomain(jpaRepository.save(entityMapper.toEntity(clinicalRecord)));
    }

    @Override
    public Optional<ClinicalRecord> findById(UUID id) {
        return jpaRepository.findById(id).map(entityMapper::toDomain);
    }

    @Override
    public Optional<ClinicalRecord> findByPatientId(UUID patientId) {
        return jpaRepository.findByPatientId(patientId).map(entityMapper::toDomain);
    }

    @Override
    public boolean existsByPatientId(UUID patientId) {
        return jpaRepository.existsByPatientId(patientId);
    }
}

