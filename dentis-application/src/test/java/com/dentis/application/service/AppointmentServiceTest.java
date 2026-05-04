package com.dentis.application.service;

import com.dentis.application.dto.request.CreateAppointmentRequest;
import com.dentis.application.dto.response.AppointmentResponse;
import com.dentis.application.mapper.AppointmentMapper;
import com.dentis.application.service.impl.AppointmentServiceImpl;
import com.dentis.common.exception.BusinessException;
import com.dentis.common.exception.ResourceNotFoundException;
import com.dentis.domain.entity.Appointment;
import com.dentis.domain.entity.Dentist;
import com.dentis.domain.entity.Patient;
import com.dentis.domain.enums.AppointmentStatus;
import com.dentis.domain.repository.AppointmentRepository;
import com.dentis.domain.repository.DentistRepository;
import com.dentis.domain.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentService")
class AppointmentServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private DentistRepository dentistRepository;
    @Mock private AppointmentMapper appointmentMapper;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private Patient patient;
    private Dentist dentist;
    private CreateAppointmentRequest validRequest;
    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        patient = Patient.builder()
                .firstName("Maria")
                .lastName("Lopez")
                .email("maria@example.com")
                .build();

        dentist = Dentist.builder()
                .firstName("Dr. Jose")
                .lastName("Martinez")
                .email("jose@clinic.com")
                .build();

        start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        end = start.plusMinutes(60);

        validRequest = CreateAppointmentRequest.builder()
                .patientId("patient-1")
                .dentistId("dentist-1")
                .startDateTime(start)
                .endDateTime(end)
                .reason("Cleaning")
                .build();
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("should create appointment when no conflicts exist")
        void shouldCreateAppointmentWithNoConflicts() {
            when(patientRepository.findById("patient-1")).thenReturn(Optional.of(patient));
            when(dentistRepository.findById("dentist-1")).thenReturn(Optional.of(dentist));
            when(appointmentRepository.findConflictingAppointments(any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            Appointment saved = Appointment.builder()
                    .patient(patient).dentist(dentist)
                    .startDateTime(start).endDateTime(end)
                    .status(AppointmentStatus.SCHEDULED).build();
            when(appointmentRepository.save(any())).thenReturn(saved);
            AppointmentResponse response = AppointmentResponse.builder()
                    .patientName("Maria Lopez").status(AppointmentStatus.SCHEDULED).build();
            when(appointmentMapper.toResponse(saved)).thenReturn(response);

            AppointmentResponse result = appointmentService.create(validRequest);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
            verify(notificationService).sendAppointmentConfirmation(any());
        }

        @Test
        @DisplayName("should throw BusinessException when appointment end is before start")
        void shouldThrowWhenEndBeforeStart() {
            validRequest.setEndDateTime(start.minusMinutes(30));
            when(patientRepository.findById("patient-1")).thenReturn(Optional.of(patient));
            when(dentistRepository.findById("dentist-1")).thenReturn(Optional.of(dentist));

            assertThatThrownBy(() -> appointmentService.create(validRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("End time must be after start time");
        }

        @Test
        @DisplayName("should throw BusinessException when duration is too short")
        void shouldThrowWhenDurationTooShort() {
            validRequest.setEndDateTime(start.plusMinutes(5));
            when(patientRepository.findById("patient-1")).thenReturn(Optional.of(patient));
            when(dentistRepository.findById("dentist-1")).thenReturn(Optional.of(dentist));

            assertThatThrownBy(() -> appointmentService.create(validRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("at least");
        }

        @Test
        @DisplayName("should throw BusinessException on scheduling conflict")
        void shouldThrowOnConflict() {
            Appointment existing = Appointment.builder()
                    .patient(patient).dentist(dentist)
                    .startDateTime(start).endDateTime(end)
                    .build();
            // Mock ID
            when(patientRepository.findById("patient-1")).thenReturn(Optional.of(patient));
            when(dentistRepository.findById("dentist-1")).thenReturn(Optional.of(dentist));
            when(appointmentRepository.findConflictingAppointments(any(), any(), any()))
                    .thenReturn(List.of(existing));

            assertThatThrownBy(() -> appointmentService.create(validRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("conflict");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when patient not found")
        void shouldThrowWhenPatientNotFound() {
            when(patientRepository.findById("patient-1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> appointmentService.create(validRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("cancel()")
    class Cancel {

        @Test
        @DisplayName("should cancel appointment and send notification")
        void shouldCancelAndNotify() {
            Appointment appointment = Appointment.builder()
                    .patient(patient).dentist(dentist)
                    .startDateTime(start).endDateTime(end)
                    .status(AppointmentStatus.SCHEDULED).build();
            when(appointmentRepository.findById("appt-1")).thenReturn(Optional.of(appointment));
            when(appointmentRepository.save(appointment)).thenReturn(appointment);

            appointmentService.cancel("appt-1");

            assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            verify(notificationService).sendAppointmentCancellation(appointment);
        }

        @Test
        @DisplayName("should throw when cancelling completed appointment")
        void shouldThrowWhenAlreadyCompleted() {
            Appointment appointment = Appointment.builder()
                    .patient(patient).dentist(dentist)
                    .startDateTime(start).endDateTime(end)
                    .status(AppointmentStatus.COMPLETED).build();
            when(appointmentRepository.findById("appt-1")).thenReturn(Optional.of(appointment));

            assertThatThrownBy(() -> appointmentService.cancel("appt-1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("completed");
        }
    }
}
