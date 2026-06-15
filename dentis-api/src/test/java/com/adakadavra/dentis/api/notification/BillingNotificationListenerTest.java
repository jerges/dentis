package com.adakadavra.dentis.api.notification;

import com.adakadavra.dentis.billing.domain.event.BudgetPresentedEvent;
import com.adakadavra.dentis.billing.domain.event.PaymentReceivedEvent;
import com.adakadavra.dentis.billing.domain.model.*;
import com.adakadavra.dentis.notification.domain.model.NotificationMessage;
import com.adakadavra.dentis.notification.domain.model.NotificationType;
import com.adakadavra.dentis.notification.domain.port.NotificationSender;
import com.adakadavra.dentis.patient.domain.model.ContactInfo;
import com.adakadavra.dentis.patient.domain.model.Patient;
import com.adakadavra.dentis.patient.domain.repository.PatientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingNotificationListener")
class BillingNotificationListenerTest {

    @Mock private NotificationSender notificationSender;
    @Mock private PatientRepository patientRepository;

    @InjectMocks
    private BillingNotificationListener listener;

    private Patient patientWithEmail(UUID id) {
        return Patient.builder()
                .id(id)
                .firstName("Ana")
                .lastName("García")
                .contactInfo(ContactInfo.builder().email("ana@example.com").build())
                .build();
    }

    private Budget buildBudget(UUID patientId) {
        BudgetItem item = BudgetItem.builder()
                .id(UUID.randomUUID())
                .tariffId(UUID.randomUUID())
                .description("Limpieza dental")
                .quantity(1)
                .unitPrice(new BigDecimal("50.00"))
                .discountPercentage(BigDecimal.ZERO)
                .paymentStatus(ProcedurePaymentStatus.PENDING)
                .build();
        return Budget.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .dentistId(UUID.randomUUID())
                .items(List.of(item))
                .status(BudgetStatus.DRAFT)
                .build();
    }

    @Nested
    @DisplayName("onBudgetPresented")
    class OnBudgetPresented {

        @Test
        @DisplayName("should send BUDGET_PRESENTED notification with correct variables")
        void shouldSendBudgetPresentedNotification() {
            UUID patientId = UUID.randomUUID();
            Budget budget = buildBudget(patientId);
            when(patientRepository.findById(patientId)).thenReturn(Optional.of(patientWithEmail(patientId)));

            listener.onBudgetPresented(new BudgetPresentedEvent(this, budget));

            ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
            verify(notificationSender).send(captor.capture());
            NotificationMessage msg = captor.getValue();
            assertThat(msg.getType()).isEqualTo(NotificationType.BUDGET_PRESENTED);
            assertThat(msg.getRecipientEmail()).isEqualTo("ana@example.com");
            assertThat(msg.getVariables()).containsKey("grandTotal");
            assertThat(msg.getVariables()).containsKey("itemCount");
        }

        @Test
        @DisplayName("should skip notification when patient has no email")
        void shouldSkipWhenNoEmail() {
            UUID patientId = UUID.randomUUID();
            Budget budget = buildBudget(patientId);
            Patient noEmail = Patient.builder()
                    .id(patientId).firstName("X").lastName("Y")
                    .contactInfo(ContactInfo.builder().email(null).build())
                    .build();
            when(patientRepository.findById(patientId)).thenReturn(Optional.of(noEmail));

            listener.onBudgetPresented(new BudgetPresentedEvent(this, budget));

            verify(notificationSender, never()).send(any());
        }

        @Test
        @DisplayName("should skip notification when patient is not found")
        void shouldSkipWhenPatientNotFound() {
            UUID patientId = UUID.randomUUID();
            Budget budget = buildBudget(patientId);
            when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

            listener.onBudgetPresented(new BudgetPresentedEvent(this, budget));

            verify(notificationSender, never()).send(any());
        }
    }

    @Nested
    @DisplayName("onPaymentReceived")
    class OnPaymentReceived {

        @Test
        @DisplayName("should send PAYMENT_RECEIVED notification with amount and method")
        void shouldSendPaymentReceivedNotification() {
            UUID patientId = UUID.randomUUID();
            Payment payment = Payment.builder()
                    .id(UUID.randomUUID())
                    .patientId(patientId)
                    .budgetId(UUID.randomUUID())
                    .amount(new BigDecimal("50.00"))
                    .paymentMethod(PaymentMethod.CASH)
                    .build();
            when(patientRepository.findById(patientId)).thenReturn(Optional.of(patientWithEmail(patientId)));

            listener.onPaymentReceived(new PaymentReceivedEvent(this, payment));

            ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
            verify(notificationSender).send(captor.capture());
            NotificationMessage msg = captor.getValue();
            assertThat(msg.getType()).isEqualTo(NotificationType.PAYMENT_RECEIVED);
            assertThat(msg.getVariables().get("amount")).isEqualTo("50.00");
            assertThat(msg.getVariables().get("paymentMethod")).isEqualTo("CASH");
        }
    }
}
