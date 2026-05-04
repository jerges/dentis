package com.dentis.application.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClinicalRecordResponse {
    private String id;
    private String patientId;
    private String patientName;
    private String generalAnamnesis;
    private String allergies;
    private String currentMedications;
    private String medicalHistory;
    private String dentalHistory;
    private List<OdontogramToothResponse> odontogram;
    private List<ClinicalEvolutionResponse> evolutions;
    private List<DiagnosisResponse> diagnoses;
    private List<TreatmentPlanSummaryResponse> treatmentPlans;
}
