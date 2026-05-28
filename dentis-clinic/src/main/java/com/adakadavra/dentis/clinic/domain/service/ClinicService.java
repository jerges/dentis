package com.adakadavra.dentis.clinic.domain.service;

import com.adakadavra.dentis.clinic.application.dto.ClinicResponse;
import com.adakadavra.dentis.clinic.application.dto.CreateClinicRequest;
import com.adakadavra.dentis.clinic.application.dto.UpdateClinicRequest;
import com.adakadavra.dentis.clinic.application.usecase.ClinicUseCase;
import com.adakadavra.dentis.clinic.domain.model.Clinic;
import com.adakadavra.dentis.clinic.domain.repository.ClinicRepository;
import com.adakadavra.dentis.common.exception.BusinessRuleException;
import com.adakadavra.dentis.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClinicService implements ClinicUseCase {

    private final ClinicRepository clinicRepository;

    @Override
    @Transactional
    public ClinicResponse createClinic(CreateClinicRequest request) {
        if (request.getNif() != null && clinicRepository.existsByNif(request.getNif())) {
            throw new BusinessRuleException("A clinic with this NIF already exists", "CLINIC_NIF_EXISTS");
        }
        Clinic clinic = Clinic.builder()
                .name(request.getName())
                .nif(request.getNif())
                .address(request.getAddress())
                .city(request.getCity())
                .province(request.getProvince())
                .zipCode(request.getZipCode())
                .phone(request.getPhone())
                .email(request.getEmail())
                .active(true)
                .build();
        return ClinicResponse.from(clinicRepository.save(clinic));
    }

    @Override
    public ClinicResponse getClinic(UUID id) {
        return clinicRepository.findById(id)
                .map(ClinicResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic", id));
    }

    @Override
    public Page<ClinicResponse> getAllClinics(Pageable pageable) {
        return clinicRepository.findAll(pageable).map(ClinicResponse::from);
    }

    @Override
    public List<ClinicResponse> getAllActiveClinics() {
        return clinicRepository.findAllActive().stream().map(ClinicResponse::from).toList();
    }

    @Override
    @Transactional
    public ClinicResponse updateClinic(UUID id, UpdateClinicRequest request) {
        Clinic existing = clinicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic", id));
        Clinic updated = existing.toBuilder()
                .name(request.getName())
                .nif(request.getNif())
                .address(request.getAddress())
                .city(request.getCity())
                .province(request.getProvince())
                .zipCode(request.getZipCode())
                .phone(request.getPhone())
                .email(request.getEmail())
                .build();
        return ClinicResponse.from(clinicRepository.save(updated));
    }

    @Override
    @Transactional
    public void deactivateClinic(UUID id) {
        if (clinicRepository.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("Clinic", id);
        }
        clinicRepository.deactivate(id);
    }
}

