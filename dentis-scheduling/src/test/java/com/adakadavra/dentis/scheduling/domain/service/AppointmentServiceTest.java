package com.adakadavra.dentis.scheduling.domain.service;

import com.adakadavra.dentis.common.exception.BusinessRuleException;
import com.adakadavra.dentis.common.exception.ResourceNotFoundException;
import com.adakadavra.dentis.scheduling.application.dto.AppointmentResponse;
import com.adakadavra.dentis.scheduling.application.dto.CreateAppointmentRequest;
import com.adakadavra.dentis.scheduling.application.mapper.AppointmentMapper;
import com.adakadavra.dentis.scheduling.domain.event.AppointmentCancelledEvent;
import com.adakadavra.dentis.scheduling.domain.event.AppointmentRescheduledEvent;
import com.adakadavra.dentis.scheduling.domain.event.AppointmentScheduledEvent;
import com.adakadavra.dentis.scheduling.domain.model.Appointment;
import com.adakadavra.dentis.scheduling.domain.model.AppointmentStatus;
import com.adakadavra.dentis.scheduling.domain.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentService")
class AppointmentServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private AppointmentMapper appointmentMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AppointmentService appointmentService;

    private UUID appointmentId;
    private UUID patientId;
    private UUID dentistId;
    private LocalDateTime start;
    private LocalDateTime end;
    private Appointment scheduledAppointment;
    private AppointmentResponse scheduledResponse;

    @BeforeEach
    void setUp() {
        appointmentId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        dentistId = UUID.randomUUID();
        start = LocalDateTime.now().plusDays(1);
        end = start.plusHours(1);

        scheduledAppointment = Appointment.builder()
                .id(appointmentId)
                .patientId(patientId)
                .dentistId(dentistId)
                .startDateTime(start)
                .endDateTime(end)
                .status(AppointmentStatus.SCHEDULED)
                .consultationReason("Routine checkup")
                .build();

        scheduledResponse = AppointmentResponse.builder()
                .id(appointmentId)
                .patientId(patientId)
                .dentistId(dentistId)
                .startDateTime(start)
                .endDateTime(end)
                .status(AppointmentStatus.SCHEDULED)
                .consultationReason("Routine checkup")
                .build();
    }

    @Nested
    @DisplayName("scheduleAppointment")
    class ScheduleAppointment {

        @Test
        @DisplayName("should be saved and event published when times are valid")
        void shouldBeSavedAndEventPublished() {
            CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                    .patientId(patientId)
                    .dentistId(dentistId)
                    .startDateTime(start)
                    .endDateTime(end)
                    .consultationReason("Routine checkup")
                    .build();

            when(appointmentMapper.toDomain(request)).thenReturn(scheduledAppointment);
            when(appointmentRepository.hasConflict(dentistId, start, end, null)).thenReturn(false);
            when(appointmentRepository.save(scheduledAppointment)).thenReturn(scheduledAppointment);
            when(appointmentMapper.toResponse(scheduledAppointment)).thenReturn(scheduledResponse);

            AppointmentResponse result = appointmentService.scheduleAppointment(request);

            assertThat(result.getId()).isEqualTo(appointmentId);
            assertThat(result.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
            verify(appointmentRepository).save(scheduledAppointment);

            ArgumentCaptor<AppointmentScheduledEvent> eventCaptor =
                    ArgumentCaptor.forClass(AppointmentScheduledEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getAppointment().getId()).isEqualTo(appointmentId);
        }

        @Test
        @DisplayName("should be rejected when end time is before start time")
        void shouldBeRejectedWhenEndTimeIsBeforeStartTime() {
            CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                    .patientId(patientId)
                    .dentistId(dentistId)
                    .startDateTime(start)
                    .endDateTime(start.minusMinutes(30))
                    .consultationReason("Checkup")
                    .build();

            assertThatThrownBy(() -> appointmentService.scheduleAppointment(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("End time must be after start time");

            verifyNoInteractions(appointmentRepository, eventPublisher);
        }

        @Test
        @DisplayName("should be rejected when end time equals start time")
        void shouldBeRejectedWhenEndTimeEqualsStartTime() {
            CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                    .patientId(patientId)
                    .dentistId(dentistId)
                    .startDateTime(start)
                    .endDateTime(start)
                    .consultationReason("Checkup")
                    .build();

            assertThatThrownBy(() -> appointmentService.scheduleAppointment(request))
                    .isInstanceOf(BusinessRuleException.class);

            verifyNoInteractions(appointmentRepository, eventPublisher);
        }

        @Test
        @DisplayName("should be rejected when dentist has a scheduling conflict")
        void shouldBeRejectedWhenDentistHasConflict() {
            CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                    .patientId(patientId)
                    .dentistId(dentistId)
                    .startDateTime(start)
                    .endDateTime(end)
                    .consultationReason("Checkup")
                    .build();

            when(appointmentRepository.hasConflict(dentistId, start, end, null)).thenReturn(true);

            assertThatThrownBy(() -> appointmentService.scheduleAppointment(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already has an appointment");

            verify(appointmentRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }
    }

    @Nested
    @DisplayName("rescheduleAppointment")
    class RescheduleAppointment {

        @Test
        @DisplayName("should be rescheduled and event published when new times are valid")
        void shouldBeRescheduledAndEventPublished() {
            LocalDateTime newStart = start.plusDays(1);
            LocalDateTime newEnd = newStart.plusHours(1);
            Appointment rescheduled = scheduledAppointment.withStartDateTime(newStart).withEndDateTime(newEnd);
            AppointmentResponse rescheduledResponse = AppointmentResponse.builder()
                    .id(appointmentId).startDateTime(newStart).endDateTime(newEnd)
                    .status(AppointmentStatus.SCHEDULED).build();

            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(scheduledAppointment));
            when(appointmentRepository.hasConflict(dentistId, newStart, newEnd, appointmentId)).thenReturn(false);
            when(appointmentRepository.save(any())).thenReturn(rescheduled);
            when(appointmentMapper.toResponse(rescheduled)).thenReturn(rescheduledResponse);

            AppointmentResponse result = appointmentService.rescheduleAppointment(appointmentId, newStart, newEnd);

            assertThat(result.getStartDateTime()).isEqualTo(newStart);
            assertThat(result.getEndDateTime()).isEqualTo(newEnd);

            ArgumentCaptor<AppointmentRescheduledEvent> eventCaptor =
                    ArgumentCaptor.forClass(AppointmentRescheduledEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
        }

        @Test
        @DisplayName("should be rejected when appointment does not exist")
        void shouldBeRejectedWhenAppointmentDoesNotExist() {
            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> appointmentService.rescheduleAppointment(appointmentId, start, end))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should be rejected when new times create a conflict")
        void shouldBeRejectedWhenNewTimesCreateConflict() {
            LocalDateTime newStart = start.plusDays(1);
            LocalDateTime newEnd = newStart.plusHours(1);

            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(scheduledAppointment));
            when(appointmentRepository.hasConflict(dentistId, newStart, newEnd, appointmentId)).thenReturn(true);

            assertThatThrownBy(() -> appointmentService.rescheduleAppointment(appointmentId, newStart, newEnd))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already has an appointment");

            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should exclude the appointment being rescheduled from conflict check")
        void shouldExcludeCurrentAppointmentFromConflictCheck() {
            LocalDateTime newStart = start.plusDays(1);
            LocalDateTime newEnd = newStart.plusHours(1);
            Appointment rescheduled = scheduledAppointment.withStartDateTime(newStart).withEndDateTime(newEnd);

            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(scheduledAppointment));
            when(appointmentRepository.hasConflict(dentistId, newStart, newEnd, appointmentId)).thenReturn(false);
            when(appointmentRepository.save(any())).thenReturn(rescheduled);
            when(appointmentMapper.toResponse(rescheduled)).thenReturn(scheduledResponse);

            appointmentService.rescheduleAppointment(appointmentId, newStart, newEnd);

            verify(appointmentRepository).hasConflict(eq(dentistId), eq(newStart), eq(newEnd), eq(appointmentId));
        }
    }

    @Nested
    @DisplayName("cancelAppointment")
    class CancelAppointment {

        @Test
        @DisplayName("should be cancelled and event published when appointment is SCHEDULED")
        void shouldBeCancelledAndEventPublished() {
            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(scheduledAppointment));
            when(appointmentMapper.toResponse(scheduledAppointment)).thenReturn(scheduledResponse);

            appointmentService.cancelAppointment(appointmentId);

            verify(appointmentRepository).cancel(appointmentId);

            ArgumentCaptor<AppointmentCancelledEvent> eventCaptor =
                    ArgumentCaptor.forClass(AppointmentCancelledEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
        }

        @Test
        @DisplayName("should be rejected when appointment is already COMPLETED")
        void shouldBeRejectedWhenAppointmentIsCompleted() {
            Appointment completedAppointment = scheduledAppointment.withStatus(AppointmentStatus.COMPLETED);
            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(completedAppointment));

            assertThatThrownBy(() -> appointmentService.cancelAppointment(appointmentId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot cancel a completed appointment");

            verify(appointmentRepository, never()).cancel(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("should be rejected when appointment is already CANCELLED")
        void shouldBeAllowedWhenAppointmentIsCancelled() {
            Appointment cancelledAppointment = scheduledAppointment.withStatus(AppointmentStatus.CANCELLED);
            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(cancelledAppointment));
            when(appointmentMapper.toResponse(cancelledAppointment)).thenReturn(scheduledResponse);

            appointmentService.cancelAppointment(appointmentId);

            verify(appointmentRepository).cancel(appointmentId);
        }

        @Test
        @DisplayName("should be rejected when appointment does not exist")
        void shouldBeRejectedWhenAppointmentDoesNotExist() {
            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> appointmentService.cancelAppointment(appointmentId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(appointmentRepository, never()).cancel(any());
        }
    }

    @Nested
    @DisplayName("confirmAppointment")
    class ConfirmAppointment {

        @Test
        @DisplayName("should be confirmed when appointment is in SCHEDULED status")
        void shouldBeConfirmedWhenScheduled() {
            Appointment confirmed = scheduledAppointment.withStatus(AppointmentStatus.CONFIRMED);
            AppointmentResponse confirmedResponse = AppointmentResponse.builder()
                    .id(appointmentId).status(AppointmentStatus.CONFIRMED).build();

            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(scheduledAppointment));
            when(appointmentRepository.save(any())).thenReturn(confirmed);
            when(appointmentMapper.toResponse(confirmed)).thenReturn(confirmedResponse);

            AppointmentResponse result = appointmentService.confirmAppointment(appointmentId);

            assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        }

        @Test
        @DisplayName("should be rejected when appointment is not in SCHEDULED status")
        void shouldBeRejectedWhenNotScheduled() {
            Appointment confirmedAppointment = scheduledAppointment.withStatus(AppointmentStatus.CONFIRMED);
            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(confirmedAppointment));

            assertThatThrownBy(() -> appointmentService.confirmAppointment(appointmentId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Only SCHEDULED appointments can be confirmed");

            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should be rejected when appointment is COMPLETED")
        void shouldBeRejectedWhenCompleted() {
            Appointment completedAppointment = scheduledAppointment.withStatus(AppointmentStatus.COMPLETED);
            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(completedAppointment));

            assertThatThrownBy(() -> appointmentService.confirmAppointment(appointmentId))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should be returned when appointment exists")
        void shouldBeReturnedWhenExists() {
            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(scheduledAppointment));
            when(appointmentMapper.toResponse(scheduledAppointment)).thenReturn(scheduledResponse);

            AppointmentResponse result = appointmentService.findById(appointmentId);

            assertThat(result.getId()).isEqualTo(appointmentId);
        }

        @Test
        @DisplayName("should throw when appointment does not exist")
        void shouldThrowWhenNotFound() {
            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> appointmentService.findById(appointmentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findByDentistAndDateRange")
    class FindByDentistAndDateRange {

        @Test
        @DisplayName("should return appointments mapped to responses")
        void shouldReturnMappedResponses() {
            when(appointmentRepository.findByDentistAndDateRange(dentistId, start, end))
                    .thenReturn(List.of(scheduledAppointment));
            when(appointmentMapper.toResponse(scheduledAppointment)).thenReturn(scheduledResponse);

            List<AppointmentResponse> result = appointmentService.findByDentistAndDateRange(dentistId, start, end);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDentistId()).isEqualTo(dentistId);
        }

        @Test
        @DisplayName("should return empty list when no appointments exist in range")
        void shouldReturnEmptyListWhenNone() {
            when(appointmentRepository.findByDentistAndDateRange(dentistId, start, end))
                    .thenReturn(List.of());

            List<AppointmentResponse> result = appointmentService.findByDentistAndDateRange(dentistId, start, end);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByPatient")
    class FindByPatient {

        @Test
        @DisplayName("should return all appointments for the patient")
        void shouldReturnAllAppointmentsForPatient() {
            when(appointmentRepository.findByPatientId(patientId))
                    .thenReturn(List.of(scheduledAppointment));
            when(appointmentMapper.toResponse(scheduledAppointment)).thenReturn(scheduledResponse);

            List<AppointmentResponse> result = appointmentService.findByPatient(patientId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPatientId()).isEqualTo(patientId);
        }
    }
}
