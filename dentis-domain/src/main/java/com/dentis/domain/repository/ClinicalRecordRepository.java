package com.dentis.domain.repository;

import com.dentis.domain.entity.ClinicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClinicalRecordRepository extends JpaRepository<ClinicalRecord, String> {

    Optional<ClinicalRecord> findByPatientId(String patientId);

    boolean existsByPatientId(String patientId);
}
