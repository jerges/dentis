package com.adakadavra.dentis.patient.application.usecase;

import com.adakadavra.dentis.patient.application.dto.CreatePatientRequest;
import com.adakadavra.dentis.patient.application.dto.PatientResponse;
import com.adakadavra.dentis.patient.application.dto.UpdatePatientRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PatientUseCase {

    PatientResponse createPatient(CreatePatientRequest request, UUID clinicId);

    PatientResponse updatePatient(UUID id, UpdatePatientRequest request);

    PatientResponse findById(UUID id);

    PatientResponse findByIdInClinic(UUID id, UUID clinicId);

    Page<PatientResponse> findAll(Pageable pageable);

    Page<PatientResponse> findAllByClinicId(UUID clinicId, Pageable pageable);

    Page<PatientResponse> searchByName(String name, Pageable pageable);

    Page<PatientResponse> searchByNameAndClinicId(String name, UUID clinicId, Pageable pageable);

    void validatePatientInClinic(UUID patientId, UUID clinicId);

    void deactivatePatient(UUID id);
}
