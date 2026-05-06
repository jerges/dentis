package com.adakadavra.dentis.clinical.domain.repository;

import com.adakadavra.dentis.clinical.domain.model.ClinicalRecord;

import java.util.Optional;
import java.util.UUID;

public interface ClinicalRecordRepository {

    ClinicalRecord save(ClinicalRecord clinicalRecord);

    Optional<ClinicalRecord> findById(UUID id);

    Optional<ClinicalRecord> findByPatientId(UUID patientId);

    boolean existsByPatientId(UUID patientId);
}
