package com.adakadavra.dentis.clinic.application.usecase;

import com.adakadavra.dentis.clinic.application.dto.ClinicResponse;
import com.adakadavra.dentis.clinic.application.dto.CreateClinicRequest;
import com.adakadavra.dentis.clinic.application.dto.UpdateClinicRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ClinicUseCase {

    ClinicResponse createClinic(CreateClinicRequest request);

    ClinicResponse getClinic(UUID id);

    Page<ClinicResponse> getAllClinics(Pageable pageable);

    List<ClinicResponse> getAllActiveClinics();

    ClinicResponse updateClinic(UUID id, UpdateClinicRequest request);

    void deactivateClinic(UUID id);
}

