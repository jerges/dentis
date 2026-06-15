package com.adakadavra.dentis.patient.domain.service;

import com.adakadavra.dentis.common.exception.BusinessRuleException;
import com.adakadavra.dentis.common.exception.ResourceNotFoundException;
import com.adakadavra.dentis.patient.application.dto.CreatePatientRequest;
import com.adakadavra.dentis.patient.application.dto.PatientResponse;
import com.adakadavra.dentis.patient.application.dto.UpdatePatientRequest;
import com.adakadavra.dentis.patient.application.mapper.PatientMapper;
import com.adakadavra.dentis.patient.application.usecase.PatientUseCase;
import com.adakadavra.dentis.patient.domain.model.Patient;
import com.adakadavra.dentis.patient.domain.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientService implements PatientUseCase {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    @Override
    @Transactional
    public PatientResponse createPatient(CreatePatientRequest request, UUID clinicId) {
        if (patientRepository.existsByIdDocument(request.getIdDocument())) {
            throw new BusinessRuleException(
                "A patient with ID document '" + request.getIdDocument() + "' already exists",
                "DUPLICATE_ID_DOCUMENT"
            );
        }
        Patient patient = patientMapper.toDomain(request);
        if (clinicId != null) {
            patient = patient.toBuilder().clinicId(clinicId).build();
        }
        Patient saved = patientRepository.save(patient);
        return patientMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PatientResponse updatePatient(UUID id, UpdatePatientRequest request) {
        Patient existing = findPatientOrThrow(id);
        Patient updated = applyUpdates(existing, request);
        Patient saved = patientRepository.save(updated);
        return patientMapper.toResponse(saved);
    }

    @Override
    public PatientResponse findById(UUID id) {
        return patientMapper.toResponse(findPatientOrThrow(id));
    }

    @Override
    public PatientResponse findByIdInClinic(UUID id, UUID clinicId) {
        Patient patient = patientRepository.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", id));
        return patientMapper.toResponse(patient);
    }

    @Override
    public Page<PatientResponse> findAll(Pageable pageable) {
        return patientRepository.findAll(pageable).map(patientMapper::toResponse);
    }

    @Override
    public Page<PatientResponse> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return patientRepository.findAllByClinicId(clinicId, pageable).map(patientMapper::toResponse);
    }

    @Override
    public Page<PatientResponse> searchByName(String name, Pageable pageable) {
        return patientRepository.searchByName(name, pageable).map(patientMapper::toResponse);
    }

    @Override
    public Page<PatientResponse> searchByNameAndClinicId(String name, UUID clinicId, Pageable pageable) {
        return patientRepository.searchByNameAndClinicId(name, clinicId, pageable).map(patientMapper::toResponse);
    }

    @Override
    public void validatePatientInClinic(UUID patientId, UUID clinicId) {
        if (!patientRepository.findByIdAndClinicId(patientId, clinicId).isPresent()) {
            throw new ResourceNotFoundException("Patient", patientId);
        }
    }

    @Override
    @Transactional
    public void deactivatePatient(UUID id) {
        findPatientOrThrow(id);
        patientRepository.deactivate(id);
    }

    private Patient findPatientOrThrow(UUID id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", id));
    }

    private Patient applyUpdates(Patient existing, UpdatePatientRequest request) {
        return existing.toBuilder()
                .firstName(request.getFirstName() != null ? request.getFirstName() : existing.getFirstName())
                .lastName(request.getLastName() != null ? request.getLastName() : existing.getLastName())
                .birthDate(request.getBirthDate() != null ? request.getBirthDate() : existing.getBirthDate())
                .sex(request.getSex() != null ? request.getSex() : existing.getSex())
                .gender(request.getGender() != null ? request.getGender() : existing.getGender())
                .socialName(request.getSocialName() != null ? request.getSocialName() : existing.getSocialName())
                .contactInfo(request.getContactInfo() != null
                        ? patientMapper.toDomain(request.getContactInfo()) : existing.getContactInfo())
                .address(request.getAddress() != null
                        ? patientMapper.toDomain(request.getAddress()) : existing.getAddress())
                .representative(request.getRepresentative() != null
                        ? patientMapper.toDomain(request.getRepresentative()) : existing.getRepresentative())
                .build();
    }
}
