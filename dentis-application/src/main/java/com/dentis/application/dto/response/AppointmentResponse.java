package com.dentis.application.dto.response;

import com.dentis.domain.enums.AppointmentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
public class AppointmentResponse {
    private String id;
    private String patientId;
    private String patientName;
    private String dentistId;
    private String dentistName;
    private String dentistColorCode;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private int durationMinutes;
    private AppointmentStatus status;
    private String reason;
    private String notes;
    private String treatmentPlanId;
    private boolean reminderSent;
    private Instant createdAt;
}
