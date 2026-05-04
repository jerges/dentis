package com.dentis.infrastructure.notification;

import com.dentis.application.service.NotificationService;
import com.dentis.domain.entity.Appointment;
import com.dentis.domain.entity.Notification;
import com.dentis.domain.enums.NotificationType;
import com.dentis.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService implements NotificationService {

    private static final int MAX_RETRIES = 3;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;

    @Override
    @Async
    public void sendAppointmentConfirmation(Appointment appointment) {
        if (appointment.getPatient().getEmail() == null) return;

        String subject = "Confirmación de cita - Dentis";
        String body = buildConfirmationBody(appointment);
        Notification notification = buildNotification(appointment, NotificationType.APPOINTMENT_CONFIRMATION, subject, body);
        sendAndSave(notification);
    }

    @Override
    @Async
    public void sendAppointmentReminder(Appointment appointment) {
        if (appointment.getPatient().getEmail() == null) return;

        String subject = "Recordatorio de cita - Dentis";
        String body = buildReminderBody(appointment);
        Notification notification = buildNotification(appointment, NotificationType.APPOINTMENT_REMINDER, subject, body);
        sendAndSave(notification);
        appointment.setReminderSent(true);
    }

    @Override
    @Async
    public void sendAppointmentCancellation(Appointment appointment) {
        if (appointment.getPatient().getEmail() == null) return;

        String subject = "Cita cancelada - Dentis";
        String body = buildCancellationBody(appointment);
        Notification notification = buildNotification(appointment, NotificationType.APPOINTMENT_CANCELLATION, subject, body);
        sendAndSave(notification);
    }

    @Override
    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void processUnsentNotifications() {
        List<Notification> pending = notificationRepository.findBySentFalseAndRetryCountLessThan(MAX_RETRIES);
        pending.forEach(this::retrySend);
    }

    private void sendAndSave(Notification notification) {
        try {
            send(notification.getRecipientEmail(), notification.getSubject(), notification.getBody());
            notification.setSent(true);
            notification.setSentAt(Instant.now());
        } catch (Exception e) {
            log.error("Failed to send notification to {}: {}", notification.getRecipientEmail(), e.getMessage());
            notification.setRetryCount(notification.getRetryCount() + 1);
            notification.setFailureReason(e.getMessage());
        }
        notificationRepository.save(notification);
    }

    private void retrySend(Notification notification) {
        try {
            send(notification.getRecipientEmail(), notification.getSubject(), notification.getBody());
            notification.setSent(true);
            notification.setSentAt(Instant.now());
            notification.setFailureReason(null);
        } catch (Exception e) {
            notification.setRetryCount(notification.getRetryCount() + 1);
            notification.setFailureReason(e.getMessage());
        }
        notificationRepository.save(notification);
    }

    private void send(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
    }

    private Notification buildNotification(Appointment appointment, NotificationType type,
                                            String subject, String body) {
        return Notification.builder()
                .patient(appointment.getPatient())
                .appointment(appointment)
                .type(type)
                .subject(subject)
                .body(body)
                .recipientEmail(appointment.getPatient().getEmail())
                .build();
    }

    private String buildConfirmationBody(Appointment appointment) {
        return "<html><body>" +
                "<h2>Confirmación de Cita</h2>" +
                "<p>Estimado/a <strong>" + appointment.getPatient().getFullName() + "</strong>,</p>" +
                "<p>Su cita ha sido confirmada con los siguientes detalles:</p>" +
                "<ul>" +
                "<li><strong>Dentista:</strong> Dr/Dra. " + appointment.getDentist().getFullName() + "</li>" +
                "<li><strong>Fecha y hora:</strong> " + appointment.getStartDateTime().format(FORMATTER) + "</li>" +
                "<li><strong>Motivo:</strong> " + (appointment.getReason() != null ? appointment.getReason() : "Consulta general") + "</li>" +
                "</ul>" +
                "<p>Si necesita cancelar o reprogramar, comuníquese con nosotros con anticipación.</p>" +
                "</body></html>";
    }

    private String buildReminderBody(Appointment appointment) {
        return "<html><body>" +
                "<h2>Recordatorio de Cita</h2>" +
                "<p>Estimado/a <strong>" + appointment.getPatient().getFullName() + "</strong>,</p>" +
                "<p>Le recordamos que tiene una cita programada para mañana:</p>" +
                "<ul>" +
                "<li><strong>Dentista:</strong> Dr/Dra. " + appointment.getDentist().getFullName() + "</li>" +
                "<li><strong>Fecha y hora:</strong> " + appointment.getStartDateTime().format(FORMATTER) + "</li>" +
                "</ul>" +
                "</body></html>";
    }

    private String buildCancellationBody(Appointment appointment) {
        return "<html><body>" +
                "<h2>Cita Cancelada</h2>" +
                "<p>Estimado/a <strong>" + appointment.getPatient().getFullName() + "</strong>,</p>" +
                "<p>Le informamos que su cita del <strong>" +
                appointment.getStartDateTime().format(FORMATTER) + "</strong> ha sido cancelada.</p>" +
                "<p>Por favor, comuníquese con nosotros para reagendar.</p>" +
                "</body></html>";
    }
}
