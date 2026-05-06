package com.adakadavra.dentis.patient.infrastructure.persistence.repository;

import com.adakadavra.dentis.patient.domain.model.Patient;
import com.adakadavra.dentis.patient.domain.repository.PatientRepository;
import com.adakadavra.dentis.patient.infrastructure.persistence.entity.PatientEntity;
import com.adakadavra.dentis.patient.infrastructure.persistence.mapper.PatientEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PatientRepositoryAdapter implements PatientRepository {

    private final PatientJpaRepository jpaRepository;
    private final PatientEntityMapper entityMapper;

    @Override
    public Patient save(Patient patient) {
        PatientEntity entity = entityMapper.toEntity(patient);
        return entityMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Patient> findById(UUID id) {
        return jpaRepository.findById(id).map(entityMapper::toDomain);
    }

    @Override
    public Optional<Patient> findByIdDocument(String idDocument) {
        return jpaRepository.findByIdDocument(idDocument).map(entityMapper::toDomain);
    }

    @Override
    public Page<Patient> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(entityMapper::toDomain);
    }

    @Override
    public Page<Patient> searchByName(String name, Pageable pageable) {
        return jpaRepository.searchByName(name, pageable).map(entityMapper::toDomain);
    }

    @Override
    public boolean existsByIdDocument(String idDocument) {
        return jpaRepository.existsByIdDocument(idDocument);
    }

    @Override
    public void deactivate(UUID id) {
        jpaRepository.deactivateById(id);
    }

    @Override
    public Optional<Patient> findByIdAndClinicId(UUID id, UUID clinicId) {
        return jpaRepository.findByIdAndClinicId(id, clinicId).map(entityMapper::toDomain);
    }

    @Override
    public Page<Patient> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return jpaRepository.findAllByClinicId(clinicId, pageable).map(entityMapper::toDomain);
    }

    @Override
    public Page<Patient> searchByNameAndClinicId(String name, UUID clinicId, Pageable pageable) {
        return jpaRepository.searchByNameAndClinicId(name, clinicId, pageable).map(entityMapper::toDomain);
    }

    @Override
    public boolean existsByIdDocumentAndClinicId(String idDocument, UUID clinicId) {
        return jpaRepository.existsByIdDocumentAndClinicId(idDocument, clinicId);
    }
}
