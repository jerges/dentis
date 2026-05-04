package com.dentis.application.service;

import com.dentis.application.dto.request.CreateClinicalEvolutionRequest;
import com.dentis.application.dto.request.UpdateOdontogramRequest;
import com.dentis.application.dto.response.ClinicalEvolutionResponse;
import com.dentis.application.dto.response.ClinicalRecordResponse;
import com.dentis.application.dto.response.OdontogramToothResponse;

public interface ClinicalRecordService {

    ClinicalRecordResponse findByPatient(String patientId);

    OdontogramToothResponse updateTooth(String patientId, UpdateOdontogramRequest request);

    ClinicalEvolutionResponse addEvolution(CreateClinicalEvolutionRequest request);

    ClinicalRecordResponse initializeForPatient(String patientId);
}
