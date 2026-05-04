package com.dentis.application.dto.request;

import com.dentis.domain.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UpdateAppointmentRequest {

    @NotNull
    private LocalDateTime startDateTime;

    @NotNull
    private LocalDateTime endDateTime;

    private AppointmentStatus status;

    private String reason;

    private String notes;

    private String dentistId;
}
