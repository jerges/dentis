package com.dentis.application.dto.response;

import com.dentis.domain.enums.TreatmentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class TreatmentPlanSummaryResponse {
    private String id;
    private String title;
    private TreatmentStatus status;
    private LocalDate startDate;
    private LocalDate estimatedEndDate;
    private String dentistName;
}
