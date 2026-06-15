package com.adakadavra.dentis.scheduling.domain.repository;

import com.adakadavra.dentis.scheduling.domain.model.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository {

    Appointment save(Appointment appointment);

    Optional<Appointment> findById(UUID id);

    List<Appointment> findByDentistAndDateRange(UUID dentistId, LocalDateTime from, LocalDateTime to);

    List<Appointment> findByPatientId(UUID patientId);

    Page<Appointment> findByDentistId(UUID dentistId, Pageable pageable);

    List<Appointment> findScheduledBetween(LocalDateTime from, LocalDateTime to);

    boolean hasConflict(UUID dentistId, LocalDateTime start, LocalDateTime end, UUID excludeAppointmentId);

    void cancel(UUID id);
}
