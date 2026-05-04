package com.dentis.application.service;

import com.dentis.application.dto.request.CreatePatientRequest;
import com.dentis.application.dto.request.UpdatePatientRequest;
import com.dentis.application.dto.response.PatientResponse;
import com.dentis.application.dto.response.PatientSummaryResponse;
import com.dentis.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface PatientService {

    PatientResponse create(CreatePatientRequest request);

    PatientResponse update(String id, UpdatePatientRequest request);

    PatientResponse findById(String id);

    PageResponse<PatientSummaryResponse> findAll(Pageable pageable);

    PageResponse<PatientSummaryResponse> search(String query, Pageable pageable);

    void deactivate(String id);
}
