package com.adakadavra.dentis.scheduling.application.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CreateAppointmentRequest {

    @NotNull
    private final UUID patientId;

    @NotNull
    private final UUID dentistId;

    @NotNull
    @Future
    private final LocalDateTime startDateTime;

    @NotNull
    @Future
    private final LocalDateTime endDateTime;

    @NotBlank
    private final String consultationReason;

    private final String notes;
}
