package com.adakadavra.dentis.scheduling.application.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAppointmentRequest {

    @NotNull
    private UUID patientId;

    @NotNull
    private UUID dentistId;

    @NotNull
    @Future
    private LocalDateTime startDateTime;

    @NotNull
    @Future
    private LocalDateTime endDateTime;

    @NotBlank
    private String consultationReason;

    private String notes;
}
