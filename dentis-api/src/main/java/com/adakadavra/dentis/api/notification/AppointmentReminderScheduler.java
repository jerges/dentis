package com.adakadavra.dentis.api.notification;

import com.adakadavra.dentis.notification.domain.model.NotificationMessage;
import com.adakadavra.dentis.notification.domain.model.NotificationType;
import com.adakadavra.dentis.notification.domain.port.NotificationSender;
import com.adakadavra.dentis.patient.domain.model.Patient;
import com.adakadavra.dentis.patient.domain.repository.PatientRepository;
import com.adakadavra.dentis.scheduling.domain.model.Appointment;
import com.adakadavra.dentis.scheduling.domain.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentReminderScheduler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final NotificationSender notificationSender;

    /** Runs every hour; sends reminders for appointments starting in the next 24-25 h window. */
    @Scheduled(fixedDelay = 3_600_000)
    public void sendReminders() {
        LocalDateTime from = LocalDateTime.now().plusHours(24);
        LocalDateTime to = LocalDateTime.now().plusHours(25);
        List<Appointment> upcoming = appointmentRepository.findScheduledBetween(from, to);
        log.info("Reminder scheduler: {} appointments in 24h window", upcoming.size());

        for (Appointment appointment : upcoming) {
            Optional<Patient> patientOpt = patientRepository.findById(appointment.getPatientId());
            if (patientOpt.isEmpty()) continue;
            Patient patient = patientOpt.get();
            if (patient.getContactInfo() == null || patient.getContactInfo().getEmail() == null
                    || patient.getContactInfo().getEmail().isBlank()) continue;

            NotificationMessage message = NotificationMessage.builder()
                    .recipientEmail(patient.getContactInfo().getEmail())
                    .recipientName(patient.fullName())
                    .type(NotificationType.APPOINTMENT_REMINDER)
                    .variables(Map.of(
                            "appointmentDate", appointment.getStartDateTime().format(FORMATTER),
                            "consultationReason", appointment.getConsultationReason() != null
                                    ? appointment.getConsultationReason() : ""
                    ))
                    .build();

            try {
                notificationSender.send(message);
            } catch (Exception e) {
                log.error("Reminder failed for patient {}", patient.getId(), e);
            }
        }
    }
}
