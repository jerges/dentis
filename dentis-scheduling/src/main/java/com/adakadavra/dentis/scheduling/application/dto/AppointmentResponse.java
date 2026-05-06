package com.adakadavra.dentis.scheduling.application.dto;

import com.adakadavra.dentis.scheduling.domain.model.AppointmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AppointmentResponse {

    private final UUID id;
    private final UUID patientId;
    private final UUID dentistId;
    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;
    private final AppointmentStatus status;
    private final String consultationReason;
    private final String notes;
}
