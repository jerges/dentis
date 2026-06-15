package com.adakadavra.dentis.api.notification;

import com.adakadavra.dentis.notification.domain.model.NotificationMessage;
import com.adakadavra.dentis.notification.domain.model.NotificationType;
import com.adakadavra.dentis.notification.domain.port.NotificationSender;
import com.adakadavra.dentis.patient.domain.model.Patient;
import com.adakadavra.dentis.patient.domain.repository.PatientRepository;
import com.adakadavra.dentis.scheduling.application.dto.AppointmentResponse;
import com.adakadavra.dentis.scheduling.domain.event.AppointmentCancelledEvent;
import com.adakadavra.dentis.scheduling.domain.event.AppointmentRescheduledEvent;
import com.adakadavra.dentis.scheduling.domain.event.AppointmentScheduledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentNotificationListener {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NotificationSender notificationSender;
    private final PatientRepository patientRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppointmentScheduled(AppointmentScheduledEvent event) {
        sendNotification(event.getAppointment(), NotificationType.APPOINTMENT_CONFIRMATION);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppointmentCancelled(AppointmentCancelledEvent event) {
        sendNotification(event.getAppointment(), NotificationType.APPOINTMENT_CANCELLATION);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppointmentRescheduled(AppointmentRescheduledEvent event) {
        sendNotification(event.getAppointment(), NotificationType.APPOINTMENT_RESCHEDULED);
    }

    private void sendNotification(AppointmentResponse appointment, NotificationType type) {
        Optional<Patient> patientOpt = patientRepository.findById(appointment.getPatientId());
        if (patientOpt.isEmpty()) {
            log.warn("Cannot send {} notification: patient {} not found", type, appointment.getPatientId());
            return;
        }
        Patient patient = patientOpt.get();
        if (patient.getContactInfo() == null || patient.getContactInfo().getEmail() == null
                || patient.getContactInfo().getEmail().isBlank()) {
            log.info("Skipping {} notification: patient {} has no email", type, patient.getId());
            return;
        }

        NotificationMessage message = NotificationMessage.builder()
                .recipientEmail(patient.getContactInfo().getEmail())
                .recipientName(patient.fullName())
                .type(type)
                .variables(Map.of(
                        "appointmentDate", appointment.getStartDateTime() != null
                                ? appointment.getStartDateTime().format(FORMATTER) : "",
                        "appointmentEndDate", appointment.getEndDateTime() != null
                                ? appointment.getEndDateTime().format(FORMATTER) : "",
                        "consultationReason", appointment.getConsultationReason() != null
                                ? appointment.getConsultationReason() : ""
                ))
                .build();

        try {
            notificationSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send {} notification to {}", type, patient.getContactInfo().getEmail(), e);
        }
    }
}
