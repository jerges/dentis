package com.adakadavra.dentis.patient.domain.repository;

import com.adakadavra.dentis.patient.domain.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface PatientRepository {

    Patient save(Patient patient);

    Optional<Patient> findById(UUID id);

    Optional<Patient> findByIdDocument(String idDocument);

    Optional<Patient> findByIdAndClinicId(UUID id, UUID clinicId);

    Page<Patient> findAll(Pageable pageable);

    Page<Patient> findAllByClinicId(UUID clinicId, Pageable pageable);

    Page<Patient> searchByName(String name, Pageable pageable);

    Page<Patient> searchByNameAndClinicId(String name, UUID clinicId, Pageable pageable);

    boolean existsByIdDocument(String idDocument);

    boolean existsByIdDocumentAndClinicId(String idDocument, UUID clinicId);

    void deactivate(UUID id);
}
