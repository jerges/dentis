package com.adakadavra.dentis.api.notification;

import com.adakadavra.dentis.billing.domain.event.BudgetPresentedEvent;
import com.adakadavra.dentis.billing.domain.event.PaymentReceivedEvent;
import com.adakadavra.dentis.billing.domain.model.Budget;
import com.adakadavra.dentis.billing.domain.model.Payment;
import com.adakadavra.dentis.notification.domain.model.NotificationMessage;
import com.adakadavra.dentis.notification.domain.model.NotificationType;
import com.adakadavra.dentis.notification.domain.port.NotificationSender;
import com.adakadavra.dentis.patient.domain.model.Patient;
import com.adakadavra.dentis.patient.domain.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingNotificationListener {

    private final NotificationSender notificationSender;
    private final PatientRepository patientRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBudgetPresented(BudgetPresentedEvent event) {
        Budget budget = event.getBudget();
        sendToPatient(budget.getPatientId(), NotificationType.BUDGET_PRESENTED, Map.of(
                "grandTotal", budget.grandTotal().toPlainString(),
                "itemCount",  String.valueOf(budget.getItems().size())
        ));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentReceived(PaymentReceivedEvent event) {
        Payment payment = event.getPayment();
        sendToPatient(payment.getPatientId(), NotificationType.PAYMENT_RECEIVED, Map.of(
                "amount",        payment.getAmount().toPlainString(),
                "paymentMethod", payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : ""
        ));
    }

    private void sendToPatient(java.util.UUID patientId, NotificationType type, Map<String, Object> vars) {
        Optional<Patient> patientOpt = patientRepository.findById(patientId);
        if (patientOpt.isEmpty()) {
            log.warn("Cannot send {} notification: patient {} not found", type, patientId);
            return;
        }
        Patient patient = patientOpt.get();
        if (patient.getContactInfo() == null
                || patient.getContactInfo().getEmail() == null
                || patient.getContactInfo().getEmail().isBlank()) {
            log.info("Skipping {} notification: patient {} has no email", type, patientId);
            return;
        }
        NotificationMessage message = NotificationMessage.builder()
                .recipientEmail(patient.getContactInfo().getEmail())
                .recipientName(patient.fullName())
                .type(type)
                .variables(vars)
                .build();
        try {
            notificationSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send {} notification to {}", type, patient.getContactInfo().getEmail(), e);
        }
    }
}
