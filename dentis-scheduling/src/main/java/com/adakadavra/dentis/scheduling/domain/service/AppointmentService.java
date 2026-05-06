package com.adakadavra.dentis.scheduling.domain.service;

import com.adakadavra.dentis.common.exception.BusinessRuleException;
import com.adakadavra.dentis.common.exception.ResourceNotFoundException;
import com.adakadavra.dentis.scheduling.application.dto.AppointmentResponse;
import com.adakadavra.dentis.scheduling.application.dto.CreateAppointmentRequest;
import com.adakadavra.dentis.scheduling.application.mapper.AppointmentMapper;
import com.adakadavra.dentis.scheduling.domain.model.Appointment;
import com.adakadavra.dentis.scheduling.domain.model.AppointmentStatus;
import com.adakadavra.dentis.scheduling.domain.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;

    @Transactional
    public AppointmentResponse scheduleAppointment(CreateAppointmentRequest request) {
        validateAppointmentTimes(request.getStartDateTime(), request.getEndDateTime());
        checkForSchedulingConflicts(request.getDentistId(), request.getStartDateTime(), request.getEndDateTime(), null);

        Appointment appointment = appointmentMapper.toDomain(request);
        Appointment saved = appointmentRepository.save(appointment);
        return appointmentMapper.toResponse(saved);
    }

    @Transactional
    public AppointmentResponse rescheduleAppointment(UUID id, LocalDateTime newStart, LocalDateTime newEnd) {
        Appointment existing = findAppointmentOrThrow(id);
        validateAppointmentTimes(newStart, newEnd);
        checkForSchedulingConflicts(existing.getDentistId(), newStart, newEnd, id);

        Appointment rescheduled = existing.withStartDateTime(newStart).withEndDateTime(newEnd);
        return appointmentMapper.toResponse(appointmentRepository.save(rescheduled));
    }

    @Transactional
    public void cancelAppointment(UUID id) {
        Appointment appointment = findAppointmentOrThrow(id);
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot cancel a completed appointment", "APPOINTMENT_ALREADY_COMPLETED");
        }
        appointmentRepository.cancel(id);
    }

    @Transactional
    public AppointmentResponse confirmAppointment(UUID id) {
        Appointment appointment = findAppointmentOrThrow(id);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BusinessRuleException("Only SCHEDULED appointments can be confirmed", "INVALID_STATUS_TRANSITION");
        }
        Appointment confirmed = appointment.withStatus(AppointmentStatus.CONFIRMED);
        return appointmentMapper.toResponse(appointmentRepository.save(confirmed));
    }

    public AppointmentResponse findById(UUID id) {
        return appointmentMapper.toResponse(findAppointmentOrThrow(id));
    }

    public List<AppointmentResponse> findByDentistAndDateRange(UUID dentistId, LocalDateTime from, LocalDateTime to) {
        return appointmentRepository.findByDentistAndDateRange(dentistId, from, to)
                .stream()
                .map(appointmentMapper::toResponse)
                .toList();
    }

    public List<AppointmentResponse> findByPatient(UUID patientId) {
        return appointmentRepository.findByPatientId(patientId)
                .stream()
                .map(appointmentMapper::toResponse)
                .toList();
    }

    private void validateAppointmentTimes(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new BusinessRuleException("End time must be after start time", "INVALID_APPOINTMENT_DURATION");
        }
    }

    private void checkForSchedulingConflicts(UUID dentistId, LocalDateTime start, LocalDateTime end, UUID excludeId) {
        if (appointmentRepository.hasConflict(dentistId, start, end, excludeId)) {
            throw new BusinessRuleException(
                "The dentist already has an appointment in the requested time slot",
                "SCHEDULING_CONFLICT"
            );
        }
    }

    private Appointment findAppointmentOrThrow(UUID id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
    }
}
