package com.dentis.application.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DiagnosisResponse {
    private String id;
    private String dentistId;
    private String dentistName;
    private String title;
    private String description;
    private String icdCode;
    private LocalDate diagnosisDate;
    private boolean active;
}
