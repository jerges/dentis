package com.adakadavra.dentis.clinical.domain.service;

import com.adakadavra.dentis.clinical.domain.model.*;
import com.adakadavra.dentis.clinical.domain.repository.ClinicalRecordRepository;
import com.adakadavra.dentis.common.exception.BusinessRuleException;
import com.adakadavra.dentis.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClinicalRecordService {

    private final ClinicalRecordRepository clinicalRecordRepository;

    @Transactional
    public ClinicalRecord createForPatient(UUID patientId) {
        if (clinicalRecordRepository.existsByPatientId(patientId)) {
            throw new BusinessRuleException(
                "Clinical record already exists for patient " + patientId,
                "DUPLICATE_CLINICAL_RECORD"
            );
        }
        ClinicalRecord record = ClinicalRecord.builder()
                .patientId(patientId)
                .dentitionType(DentitionType.PERMANENT)
                .createdAt(LocalDateTime.now())
                .odontogram(new ArrayList<>())
                .evolutions(new ArrayList<>())
                .diagnoses(new ArrayList<>())
                .treatmentPlans(new ArrayList<>())
                .build();
        return clinicalRecordRepository.save(record);
    }

    public ClinicalRecord findByPatientId(UUID patientId) {
        return clinicalRecordRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("ClinicalRecord for patient", patientId));
    }

    @Transactional
    public ClinicalRecord updateOdontogram(UUID patientId, List<OdontogramTooth> teeth) {
        ClinicalRecord record = findByPatientId(patientId);
        ClinicalRecord updated = record.withOdontogram(teeth).withUpdatedAt(LocalDateTime.now());
        return clinicalRecordRepository.save(updated);
    }

    @Transactional
    public ClinicalRecord addEvolution(UUID patientId, ClinicalEvolution evolution) {
        ClinicalRecord record = findByPatientId(patientId);
        List<ClinicalEvolution> evolutions = new ArrayList<>(record.getEvolutions());
        evolutions.add(evolution);
        ClinicalRecord updated = record.withEvolutions(evolutions).withUpdatedAt(LocalDateTime.now());
        return clinicalRecordRepository.save(updated);
    }

    @Transactional
    public ClinicalRecord addDiagnosis(UUID patientId, Diagnosis diagnosis) {
        ClinicalRecord record = findByPatientId(patientId);
        List<Diagnosis> diagnoses = new ArrayList<>(record.getDiagnoses());
        diagnoses.add(diagnosis);
        ClinicalRecord updated = record.withDiagnoses(diagnoses).withUpdatedAt(LocalDateTime.now());
        return clinicalRecordRepository.save(updated);
    }

    @Transactional
    public ClinicalRecord addTreatmentPlan(UUID patientId, TreatmentPlan plan) {
        ClinicalRecord record = findByPatientId(patientId);
        List<TreatmentPlan> plans = new ArrayList<>(record.getTreatmentPlans());
        plans.add(plan);
        ClinicalRecord updated = record.withTreatmentPlans(plans).withUpdatedAt(LocalDateTime.now());
        return clinicalRecordRepository.save(updated);
    }

    @Transactional
    public ClinicalRecord updateDentitionType(UUID patientId, DentitionType dentitionType) {
        ClinicalRecord record = findByPatientId(patientId);
        ClinicalRecord updated = record.withDentitionType(dentitionType).withUpdatedAt(LocalDateTime.now());
        return clinicalRecordRepository.save(updated);
    }

    @Transactional
    public ClinicalRecord getOrCreateForPatient(UUID patientId) {
        return clinicalRecordRepository.findByPatientId(patientId)
                .orElseGet(() -> createForPatient(patientId));
    }
}
