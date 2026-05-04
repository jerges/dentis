package com.dentis.application.service.impl;

import com.dentis.application.dto.request.CreateClinicalEvolutionRequest;
import com.dentis.application.dto.request.UpdateOdontogramRequest;
import com.dentis.application.dto.response.ClinicalEvolutionResponse;
import com.dentis.application.dto.response.ClinicalRecordResponse;
import com.dentis.application.dto.response.OdontogramToothResponse;
import com.dentis.application.mapper.AppointmentMapper;
import com.dentis.application.service.ClinicalRecordService;
import com.dentis.common.exception.BusinessException;
import com.dentis.common.exception.ResourceNotFoundException;
import com.dentis.domain.entity.*;
import com.dentis.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClinicalRecordServiceImpl implements ClinicalRecordService {

    private final ClinicalRecordRepository clinicalRecordRepository;
    private final PatientRepository patientRepository;
    private final DentistRepository dentistRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    public ClinicalRecordResponse findByPatient(String patientId) {
        ClinicalRecord record = clinicalRecordRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("ClinicalRecord for patient", patientId));
        return toResponse(record);
    }

    @Override
    @Transactional
    public OdontogramToothResponse updateTooth(String patientId, UpdateOdontogramRequest request) {
        ClinicalRecord record = clinicalRecordRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("ClinicalRecord for patient", patientId));

        Optional<OdontogramTooth> existing = record.getOdontogram().stream()
                .filter(t -> t.getToothNumber().equals(request.getToothNumber()))
                .findFirst();

        OdontogramTooth tooth;
        if (existing.isPresent()) {
            tooth = existing.get();
        } else {
            tooth = OdontogramTooth.builder()
                    .clinicalRecord(record)
                    .toothNumber(request.getToothNumber())
                    .build();
            record.getOdontogram().add(tooth);
        }

        tooth.setStatus(request.getStatus());
        if (request.getAffectedSurfaces() != null) {
            tooth.setAffectedSurfaces(request.getAffectedSurfaces());
        }
        tooth.setNotes(request.getNotes());

        clinicalRecordRepository.save(record);
        return toToothResponse(tooth);
    }

    @Override
    @Transactional
    public ClinicalEvolutionResponse addEvolution(CreateClinicalEvolutionRequest request) {
        ClinicalRecord record = clinicalRecordRepository.findById(request.getClinicalRecordId())
                .orElseThrow(() -> new ResourceNotFoundException("ClinicalRecord", request.getClinicalRecordId()));

        ClinicalEvolution evolution = ClinicalEvolution.builder()
                .clinicalRecord(record)
                .evolutionDate(request.getEvolutionDate())
                .description(request.getDescription())
                .procedure(request.getProcedure())
                .observations(request.getObservations())
                .build();

        if (request.getDentistId() != null) {
            evolution.setDentist(dentistRepository.findById(request.getDentistId())
                    .orElseThrow(() -> new ResourceNotFoundException("Dentist", request.getDentistId())));
        }

        record.getEvolutions().add(evolution);
        clinicalRecordRepository.save(record);
        return toEvolutionResponse(evolution);
    }

    @Override
    @Transactional
    public ClinicalRecordResponse initializeForPatient(String patientId) {
        if (clinicalRecordRepository.existsByPatientId(patientId)) {
            throw new BusinessException("Clinical record already exists for patient " + patientId,
                    "CLINICAL_RECORD_EXISTS");
        }
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        ClinicalRecord record = ClinicalRecord.builder().patient(patient).build();
        return toResponse(clinicalRecordRepository.save(record));
    }

    private ClinicalRecordResponse toResponse(ClinicalRecord record) {
        return ClinicalRecordResponse.builder()
                .id(record.getId())
                .patientId(record.getPatient().getId())
                .patientName(record.getPatient().getFullName())
                .generalAnamnesis(record.getGeneralAnamnesis())
                .allergies(record.getAllergies())
                .currentMedications(record.getCurrentMedications())
                .medicalHistory(record.getMedicalHistory())
                .dentalHistory(record.getDentalHistory())
                .odontogram(record.getOdontogram().stream().map(this::toToothResponse).toList())
                .evolutions(record.getEvolutions().stream().map(this::toEvolutionResponse).toList())
                .build();
    }

    private OdontogramToothResponse toToothResponse(OdontogramTooth tooth) {
        return OdontogramToothResponse.builder()
                .id(tooth.getId())
                .toothNumber(tooth.getToothNumber())
                .status(tooth.getStatus())
                .affectedSurfaces(tooth.getAffectedSurfaces())
                .notes(tooth.getNotes())
                .build();
    }

    private ClinicalEvolutionResponse toEvolutionResponse(ClinicalEvolution evolution) {
        return ClinicalEvolutionResponse.builder()
                .id(evolution.getId())
                .clinicalRecordId(evolution.getClinicalRecord().getId())
                .dentistId(evolution.getDentist() != null ? evolution.getDentist().getId() : null)
                .dentistName(evolution.getDentist() != null ? evolution.getDentist().getFullName() : null)
                .evolutionDate(evolution.getEvolutionDate())
                .description(evolution.getDescription())
                .procedure(evolution.getProcedure())
                .observations(evolution.getObservations())
                .createdAt(evolution.getCreatedAt())
                .build();
    }
}
