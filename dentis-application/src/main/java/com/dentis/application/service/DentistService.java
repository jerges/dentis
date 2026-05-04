package com.dentis.application.service;

import com.dentis.application.dto.request.CreateDentistRequest;
import com.dentis.application.dto.response.DentistResponse;

import java.util.List;

public interface DentistService {

    DentistResponse create(CreateDentistRequest request);

    DentistResponse findById(String id);

    List<DentistResponse> findAllActive();

    void deactivate(String id);
}
