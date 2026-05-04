package com.dentis.application.service.impl;

import com.dentis.application.dto.request.CreatePatientRequest;
import com.dentis.application.dto.request.UpdatePatientRequest;
import com.dentis.application.dto.response.PatientResponse;
import com.dentis.application.dto.response.PatientSummaryResponse;
import com.dentis.application.mapper.PatientMapper;
import com.dentis.application.service.PatientService;
import com.dentis.common.exception.BusinessException;
import com.dentis.common.exception.ResourceNotFoundException;
import com.dentis.common.response.PageResponse;
import com.dentis.domain.entity.Patient;
import com.dentis.domain.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    @Override
    @Transactional
    public PatientResponse create(CreatePatientRequest request) {
        if (request.getDocumentId() != null && patientRepository.existsByDocumentId(request.getDocumentId())) {
            throw new BusinessException("A patient with document ID " + request.getDocumentId() + " already exists",
                    "DUPLICATE_DOCUMENT_ID");
        }
        Patient patient = patientMapper.toEntity(request);
        return patientMapper.toResponse(patientRepository.save(patient));
    }

    @Override
    @Transactional
    public PatientResponse update(String id, UpdatePatientRequest request) {
        Patient patient = findPatientOrThrow(id);
        if (request.getDocumentId() != null
                && !request.getDocumentId().equals(patient.getDocumentId())
                && patientRepository.existsByDocumentId(request.getDocumentId())) {
            throw new BusinessException("A patient with document ID " + request.getDocumentId() + " already exists",
                    "DUPLICATE_DOCUMENT_ID");
        }
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setDocumentId(request.getDocumentId());
        patient.setBirthDate(request.getBirthDate());
        patient.setGender(request.getGender());
        patient.setSocialName(request.getSocialName());
        patient.setPhone(request.getPhone());
        patient.setAltPhone(request.getAltPhone());
        patient.setEmail(request.getEmail());
        patient.setAddress(request.getAddress());
        patient.setCity(request.getCity());
        patient.setState(request.getState());
        patient.setRepresentativeName(request.getRepresentativeName());
        patient.setRepresentativePhone(request.getRepresentativePhone());
        patient.setRepresentativeRelationship(request.getRepresentativeRelationship());
        patient.setNotes(request.getNotes());
        return patientMapper.toResponse(patientRepository.save(patient));
    }

    @Override
    public PatientResponse findById(String id) {
        return patientMapper.toResponse(findPatientOrThrow(id));
    }

    @Override
    public PageResponse<PatientSummaryResponse> findAll(Pageable pageable) {
        Page<Patient> page = patientRepository.findByActiveTrue(pageable);
        return PageResponse.of(
                page.getContent().stream().map(patientMapper::toSummaryResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements()
        );
    }

    @Override
    public PageResponse<PatientSummaryResponse> search(String query, Pageable pageable) {
        Page<Patient> page = patientRepository.searchPatients(query, pageable);
        return PageResponse.of(
                page.getContent().stream().map(patientMapper::toSummaryResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements()
        );
    }

    @Override
    @Transactional
    public void deactivate(String id) {
        Patient patient = findPatientOrThrow(id);
        patient.setActive(false);
        patientRepository.save(patient);
    }

    private Patient findPatientOrThrow(String id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", id));
    }
}
