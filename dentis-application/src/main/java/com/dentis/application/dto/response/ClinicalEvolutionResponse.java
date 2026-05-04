package com.dentis.application.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class ClinicalEvolutionResponse {
    private String id;
    private String clinicalRecordId;
    private String dentistId;
    private String dentistName;
    private LocalDate evolutionDate;
    private String description;
    private String procedure;
    private String observations;
    private String appointmentId;
    private Instant createdAt;
}
