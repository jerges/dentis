package com.adakadavra.dentis.scheduling.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@With
@AllArgsConstructor
public class Appointment {

    private final UUID id;
    private final UUID patientId;
    private final UUID dentistId;
    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;
    private final AppointmentStatus status;
    private final String consultationReason;
    private final String notes;

    public boolean isActive() {
        return status == AppointmentStatus.SCHEDULED || status == AppointmentStatus.CONFIRMED;
    }

    public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return startDateTime.isBefore(otherEnd) && endDateTime.isAfter(otherStart);
    }
}
