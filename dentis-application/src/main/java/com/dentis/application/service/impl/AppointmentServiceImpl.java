package com.dentis.application.service.impl;

import com.dentis.application.dto.request.CreateAppointmentRequest;
import com.dentis.application.dto.request.UpdateAppointmentRequest;
import com.dentis.application.dto.response.AppointmentResponse;
import com.dentis.application.mapper.AppointmentMapper;
import com.dentis.application.service.AppointmentService;
import com.dentis.application.service.NotificationService;
import com.dentis.common.constant.AppConstants;
import com.dentis.common.exception.BusinessException;
import com.dentis.common.exception.ResourceNotFoundException;
import com.dentis.common.response.PageResponse;
import com.dentis.domain.entity.Appointment;
import com.dentis.domain.entity.Dentist;
import com.dentis.domain.entity.Patient;
import com.dentis.domain.enums.AppointmentStatus;
import com.dentis.domain.repository.AppointmentRepository;
import com.dentis.domain.repository.DentistRepository;
import com.dentis.domain.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DentistRepository dentistRepository;
    private final AppointmentMapper appointmentMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest request) {
        validateDateRange(request.getStartDateTime(), request.getEndDateTime());

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", request.getPatientId()));

        Dentist dentist = dentistRepository.findById(request.getDentistId())
                .orElseThrow(() -> new ResourceNotFoundException("Dentist", request.getDentistId()));

        checkForConflicts(request.getDentistId(), request.getStartDateTime(), request.getEndDateTime(), null);

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .dentist(dentist)
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .reason(request.getReason())
                .notes(request.getNotes())
                .status(AppointmentStatus.SCHEDULED)
                .build();

        appointment = appointmentRepository.save(appointment);
        notificationService.sendAppointmentConfirmation(appointment);
        return appointmentMapper.toResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse update(String id, UpdateAppointmentRequest request) {
        Appointment appointment = findOrThrow(id);
        validateNotCancelled(appointment);
        validateDateRange(request.getStartDateTime(), request.getEndDateTime());
        checkForConflicts(appointment.getDentist().getId(), request.getStartDateTime(), request.getEndDateTime(), id);

        appointment.setStartDateTime(request.getStartDateTime());
        appointment.setEndDateTime(request.getEndDateTime());
        appointment.setReason(request.getReason());
        appointment.setNotes(request.getNotes());

        if (request.getStatus() != null) {
            appointment.setStatus(request.getStatus());
        }

        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Override
    public AppointmentResponse findById(String id) {
        return appointmentMapper.toResponse(findOrThrow(id));
    }

    @Override
    public PageResponse<AppointmentResponse> findByPatient(String patientId, Pageable pageable) {
        Page<Appointment> page = appointmentRepository.findByPatientId(patientId, pageable);
        return toPageResponse(page);
    }

    @Override
    public PageResponse<AppointmentResponse> findByDentist(String dentistId, Pageable pageable) {
        Page<Appointment> page = appointmentRepository.findByDentistId(dentistId, pageable);
        return toPageResponse(page);
    }

    @Override
    public List<AppointmentResponse> findDentistCalendar(String dentistId, LocalDateTime from, LocalDateTime to) {
        List<AppointmentStatus> excluded = List.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW);
        return appointmentRepository.findDentistAppointmentsBetween(dentistId, from, to, excluded)
                .stream()
                .map(appointmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AppointmentResponse updateStatus(String id, AppointmentStatus status) {
        Appointment appointment = findOrThrow(id);
        appointment.setStatus(status);
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public void cancel(String id) {
        Appointment appointment = findOrThrow(id);
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessException("Cannot cancel a completed appointment", "APPOINTMENT_ALREADY_COMPLETED");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
        notificationService.sendAppointmentCancellation(appointment);
    }

    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new BusinessException("End time must be after start time", "INVALID_DATE_RANGE");
        }
        long duration = ChronoUnit.MINUTES.between(start, end);
        if (duration < AppConstants.Appointment.MIN_DURATION_MINUTES) {
            throw new BusinessException("Appointment duration must be at least " +
                    AppConstants.Appointment.MIN_DURATION_MINUTES + " minutes", "DURATION_TOO_SHORT");
        }
        if (duration > AppConstants.Appointment.MAX_DURATION_MINUTES) {
            throw new BusinessException("Appointment duration cannot exceed " +
                    AppConstants.Appointment.MAX_DURATION_MINUTES + " minutes", "DURATION_TOO_LONG");
        }
    }

    private void checkForConflicts(String dentistId, LocalDateTime start, LocalDateTime end, String excludeId) {
        List<Appointment> conflicts = appointmentRepository.findConflictingAppointments(dentistId, start, end);
        boolean hasConflict = conflicts.stream()
                .anyMatch(a -> !a.getId().equals(excludeId));
        if (hasConflict) {
            throw new BusinessException("The dentist already has an appointment in this time slot",
                    "APPOINTMENT_CONFLICT");
        }
    }

    private void validateNotCancelled(Appointment appointment) {
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Cannot update a cancelled appointment", "APPOINTMENT_CANCELLED");
        }
    }

    private PageResponse<AppointmentResponse> toPageResponse(Page<Appointment> page) {
        return PageResponse.of(
                page.getContent().stream().map(appointmentMapper::toResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements()
        );
    }

    private Appointment findOrThrow(String id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
    }
}
