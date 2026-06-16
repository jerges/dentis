package com.adakadavra.dentis.notification.infrastructure.email;

import com.adakadavra.dentis.notification.domain.model.NotificationMessage;
import com.adakadavra.dentis.notification.domain.model.NotificationType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailNotificationSender")
class EmailNotificationSenderTest {

    @Mock private JavaMailSender mailSender;
    @Mock private TemplateEngine templateEngine;
    @Mock private MimeMessage mimeMessage;

    @InjectMocks
    private EmailNotificationSender emailNotificationSender;

    @BeforeEach
    void setUp() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    private NotificationMessage buildMessage(NotificationType type) {
        return NotificationMessage.builder()
                .recipientEmail("paciente@example.com")
                .recipientName("Carlos Rodríguez")
                .type(type)
                .variables(Map.of("appointmentDate", "2026-06-20"))
                .build();
    }

    @Nested
    @DisplayName("send")
    class Send {

        @Test
        @DisplayName("should be sent when message is valid appointment confirmation")
        void shouldBeSentWhenValidAppointmentConfirmation() throws MessagingException {
            NotificationMessage message = buildMessage(NotificationType.APPOINTMENT_CONFIRMATION);
            when(templateEngine.process(eq("email/appointment_confirmation"), any(Context.class)))
                    .thenReturn("<html>Confirmed</html>");

            emailNotificationSender.send(message);

            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("should be sent to the recipient email address")
        void shouldBeSentToRecipientEmail() throws MessagingException {
            NotificationMessage message = buildMessage(NotificationType.APPOINTMENT_CONFIRMATION);
            when(templateEngine.process(any(String.class), any(Context.class))).thenReturn("<html>OK</html>");

            emailNotificationSender.send(message);

            verify(mailSender).send(mimeMessage);
        }

        @ParameterizedTest
        @EnumSource(NotificationType.class)
        @DisplayName("should be sent for every notification type")
        void shouldBeSentForEveryNotificationType(NotificationType type) {
            NotificationMessage message = buildMessage(type);
            when(templateEngine.process(any(String.class), any(Context.class))).thenReturn("<html>Body</html>");

            assertThatCode(() -> emailNotificationSender.send(message))
                    .doesNotThrowAnyException();

            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("should use recipient name in template context")
        void shouldUseRecipientNameInTemplateContext() {
            NotificationMessage message = buildMessage(NotificationType.BUDGET_PRESENTED);
            when(templateEngine.process(any(String.class), any(Context.class))).thenReturn("<html>Budget</html>");

            emailNotificationSender.send(message);

            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            verify(templateEngine).process(any(String.class), contextCaptor.capture());
            assertThat(contextCaptor.getValue().getVariable("recipientName")).isEqualTo("Carlos Rodríguez");
        }

        @Test
        @DisplayName("should include custom variables in template context")
        void shouldIncludeCustomVariablesInTemplateContext() {
            NotificationMessage message = NotificationMessage.builder()
                    .recipientEmail("paciente@example.com")
                    .recipientName("Carlos Rodríguez")
                    .type(NotificationType.PAYMENT_RECEIVED)
                    .variables(Map.of("amount", "100.00", "method", "CASH"))
                    .build();

            when(templateEngine.process(any(String.class), any(Context.class))).thenReturn("<html>Payment</html>");

            emailNotificationSender.send(message);

            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            verify(templateEngine).process(any(String.class), contextCaptor.capture());
            Context capturedContext = contextCaptor.getValue();
            assertThat(capturedContext.getVariable("amount")).isEqualTo("100.00");
            assertThat(capturedContext.getVariable("method")).isEqualTo("CASH");
        }

        @Test
        @DisplayName("should not throw when variables map is null")
        void shouldNotThrowWhenVariablesMapIsNull() {
            NotificationMessage message = NotificationMessage.builder()
                    .recipientEmail("paciente@example.com")
                    .recipientName("Carlos Rodríguez")
                    .type(NotificationType.APPOINTMENT_CANCELLATION)
                    .variables(null)
                    .build();

            when(templateEngine.process(any(String.class), any(Context.class))).thenReturn("<html>Cancelled</html>");

            assertThatCode(() -> emailNotificationSender.send(message))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should not propagate exception when mail server fails")
        void shouldNotPropagateWhenMailServerFails() throws MessagingException {
            NotificationMessage message = buildMessage(NotificationType.APPOINTMENT_CONFIRMATION);
            when(templateEngine.process(any(String.class), any(Context.class))).thenReturn("<html>OK</html>");
            doThrow(new MailSendException("SMTP connection refused"))
                    .when(mailSender).send(any(MimeMessage.class));

            assertThatCode(() -> emailNotificationSender.send(message))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("resolveSubject")
    class ResolveSubject {

        @Test
        @DisplayName("should resolve subject for APPOINTMENT_CONFIRMATION")
        void shouldResolveSubjectForAppointmentConfirmation() {
            NotificationMessage message = buildMessage(NotificationType.APPOINTMENT_CONFIRMATION);
            when(templateEngine.process(any(String.class), any(Context.class))).thenReturn("<html>OK</html>");

            emailNotificationSender.send(message);

            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("should use correct template name for BUDGET_PRESENTED")
        void shouldUseCorrectTemplateNameForBudgetPresented() {
            NotificationMessage message = buildMessage(NotificationType.BUDGET_PRESENTED);
            when(templateEngine.process(eq("email/budget_presented"), any(Context.class)))
                    .thenReturn("<html>Budget</html>");

            emailNotificationSender.send(message);

            verify(templateEngine).process(eq("email/budget_presented"), any(Context.class));
        }

        @Test
        @DisplayName("should use correct template name for PAYMENT_RECEIVED")
        void shouldUseCorrectTemplateNameForPaymentReceived() {
            NotificationMessage message = buildMessage(NotificationType.PAYMENT_RECEIVED);
            when(templateEngine.process(eq("email/payment_received"), any(Context.class)))
                    .thenReturn("<html>Payment</html>");

            emailNotificationSender.send(message);

            verify(templateEngine).process(eq("email/payment_received"), any(Context.class));
        }

        @Test
        @DisplayName("should use correct template name for APPOINTMENT_CANCELLATION")
        void shouldUseCorrectTemplateNameForAppointmentCancellation() {
            NotificationMessage message = buildMessage(NotificationType.APPOINTMENT_CANCELLATION);
            when(templateEngine.process(eq("email/appointment_cancellation"), any(Context.class)))
                    .thenReturn("<html>Cancelled</html>");

            emailNotificationSender.send(message);

            verify(templateEngine).process(eq("email/appointment_cancellation"), any(Context.class));
        }
    }
}
