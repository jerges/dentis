package com.dentis.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CreateAppointmentRequest {

    @NotBlank
    private String patientId;

    @NotBlank
    private String dentistId;

    @NotNull
    @Future
    private LocalDateTime startDateTime;

    @NotNull
    private LocalDateTime endDateTime;

    @Size(max = 255)
    private String reason;

    private String notes;

    private String treatmentPlanId;
}
