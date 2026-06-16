package com.adakadavra.dentis.notification.infrastructure.email;

import com.adakadavra.dentis.notification.domain.model.NotificationMessage;
import com.adakadavra.dentis.notification.domain.port.NotificationSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Async("notificationExecutor")
    @Override
    public void send(NotificationMessage message) {
        try {
            String html = renderTemplate(message);
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(message.getRecipientEmail());
            helper.setSubject(resolveSubject(message));
            helper.setText(html, true);
            mailSender.send(mimeMessage);
            log.info("Notification sent [type={}, recipient={}]", message.getType(), message.getRecipientEmail());
        } catch (MessagingException | MailException e) {
            log.error("Failed to send notification [type={}, recipient={}]", message.getType(), message.getRecipientEmail(), e);
        }
    }

    private String renderTemplate(NotificationMessage message) {
        Context context = new Context();
        context.setVariable("recipientName", message.getRecipientName());
        if (message.getVariables() != null) {
            message.getVariables().forEach(context::setVariable);
        }
        return templateEngine.process("email/" + message.getType().name().toLowerCase(), context);
    }

    private String resolveSubject(NotificationMessage message) {
        return switch (message.getType()) {
            case APPOINTMENT_CONFIRMATION -> "Appointment Confirmed - Dentis";
            case APPOINTMENT_REMINDER -> "Appointment Reminder - Dentis";
            case APPOINTMENT_CANCELLATION -> "Appointment Cancelled - Dentis";
            case APPOINTMENT_RESCHEDULED -> "Appointment Rescheduled - Dentis";
            case BUDGET_PRESENTED -> "Your Treatment Budget - Dentis";
            case PAYMENT_RECEIVED -> "Payment Received - Dentis";
        };
    }
}
