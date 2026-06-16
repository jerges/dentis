package com.adakadavra.dentis.billing.domain.service;

import com.adakadavra.dentis.billing.domain.event.PaymentReceivedEvent;
import com.adakadavra.dentis.billing.domain.model.*;
import com.adakadavra.dentis.billing.domain.repository.BudgetRepository;
import com.adakadavra.dentis.billing.domain.repository.PaymentRepository;
import com.adakadavra.dentis.common.exception.BusinessRuleException;
import com.adakadavra.dentis.common.exception.ResourceNotFoundException;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService")
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private BudgetRepository budgetRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private UUID patientId;
    private UUID budgetId;
    private Budget approvedBudget;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        budgetId = UUID.randomUUID();

        BudgetItem item = BudgetItem.builder()
                .id(UUID.randomUUID())
                .description("Limpieza dental")
                .quantity(1)
                .unitPrice(new BigDecimal("100.00"))
                .discountPercentage(BigDecimal.ZERO)
                .build();

        approvedBudget = Budget.builder()
                .id(budgetId)
                .patientId(patientId)
                .dentistId(UUID.randomUUID())
                .items(List.of(item))
                .status(BudgetStatus.APPROVED)
                .build();
    }

    private Payment buildPayment(BigDecimal amount) {
        return Payment.builder()
                .patientId(patientId)
                .budgetId(budgetId)
                .amount(amount)
                .paymentMethod(PaymentMethod.CASH)
                .build();
    }

    @Nested
    @DisplayName("registerPayment")
    class RegisterPayment {

        @Test
        @DisplayName("should be saved and event published when budget is APPROVED and amount is valid")
        void shouldBeSavedAndEventPublished() {
            Payment payment = buildPayment(new BigDecimal("50.00"));
            Payment saved = Payment.builder().id(UUID.randomUUID()).patientId(patientId)
                    .budgetId(budgetId).amount(payment.getAmount()).build();

            when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(approvedBudget));
            when(paymentRepository.sumPaymentsByBudgetId(budgetId)).thenReturn(BigDecimal.ZERO);
            when(paymentRepository.save(any())).thenReturn(saved);

            Payment result = paymentService.registerPayment(payment);

            assertThat(result).isNotNull();
            verify(paymentRepository).save(any());

            ArgumentCaptor<PaymentReceivedEvent> eventCaptor =
                    ArgumentCaptor.forClass(PaymentReceivedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
        }

        @Test
        @DisplayName("should be saved when payment equals the full remaining balance")
        void shouldBeSavedWhenPaymentEqualsFullBalance() {
            Payment payment = buildPayment(new BigDecimal("100.00"));
            Payment saved = Payment.builder().id(UUID.randomUUID()).patientId(patientId)
                    .budgetId(budgetId).amount(payment.getAmount()).build();

            when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(approvedBudget));
            when(paymentRepository.sumPaymentsByBudgetId(budgetId)).thenReturn(BigDecimal.ZERO);
            when(paymentRepository.save(any())).thenReturn(saved);

            Payment result = paymentService.registerPayment(payment);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should set paidAt timestamp automatically")
        void shouldSetPaidAtTimestampAutomatically() {
            Payment payment = buildPayment(new BigDecimal("50.00"));

            when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(approvedBudget));
            when(paymentRepository.sumPaymentsByBudgetId(budgetId)).thenReturn(BigDecimal.ZERO);
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            paymentService.registerPayment(payment);

            ArgumentCaptor<Payment> savedCaptor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(savedCaptor.capture());
            assertThat(savedCaptor.getValue().getPaidAt()).isNotNull();
        }

        @Test
        @DisplayName("should be rejected when budget does not exist")
        void shouldBeRejectedWhenBudgetDoesNotExist() {
            UUID unknownBudgetId = UUID.randomUUID();
            Payment payment = Payment.builder()
                    .budgetId(unknownBudgetId)
                    .patientId(patientId)
                    .amount(new BigDecimal("50.00"))
                    .build();

            when(budgetRepository.findById(unknownBudgetId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.registerPayment(payment))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(paymentRepository, eventPublisher);
        }

        @Test
        @DisplayName("should be rejected when budget is in DRAFT status")
        void shouldBeRejectedWhenBudgetIsDraft() {
            Budget draftBudget = approvedBudget.withStatus(BudgetStatus.DRAFT);
            Payment payment = buildPayment(new BigDecimal("50.00"));

            when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(draftBudget));

            assertThatThrownBy(() -> paymentService.registerPayment(payment))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("APPROVED budgets");

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should be rejected when budget is in PRESENTED status")
        void shouldBeRejectedWhenBudgetIsPresented() {
            Budget presentedBudget = approvedBudget.withStatus(BudgetStatus.PRESENTED);
            Payment payment = buildPayment(new BigDecimal("50.00"));

            when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(presentedBudget));

            assertThatThrownBy(() -> paymentService.registerPayment(payment))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("APPROVED budgets");
        }

        @Test
        @DisplayName("should be rejected when payment amount is zero")
        void shouldBeRejectedWhenAmountIsZero() {
            Payment payment = buildPayment(BigDecimal.ZERO);

            when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(approvedBudget));

            assertThatThrownBy(() -> paymentService.registerPayment(payment))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("must be greater than zero");

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should be rejected when payment amount is negative")
        void shouldBeRejectedWhenAmountIsNegative() {
            Payment payment = buildPayment(new BigDecimal("-10.00"));

            when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(approvedBudget));

            assertThatThrownBy(() -> paymentService.registerPayment(payment))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("must be greater than zero");
        }

        @Test
        @DisplayName("should be rejected when payment amount exceeds remaining balance")
        void shouldBeRejectedWhenAmountExceedsRemainingBalance() {
            Payment payment = buildPayment(new BigDecimal("90.00"));

            when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(approvedBudget));
            when(paymentRepository.sumPaymentsByBudgetId(budgetId)).thenReturn(new BigDecimal("50.00"));

            assertThatThrownBy(() -> paymentService.registerPayment(payment))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("exceeds remaining balance");

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should be rejected when paying more than budget total")
        void shouldBeRejectedWhenPayingMoreThanBudgetTotal() {
            Payment payment = buildPayment(new BigDecimal("150.00"));

            when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(approvedBudget));
            when(paymentRepository.sumPaymentsByBudgetId(budgetId)).thenReturn(BigDecimal.ZERO);

            assertThatThrownBy(() -> paymentService.registerPayment(payment))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("exceeds remaining balance");
        }
    }

    @Nested
    @DisplayName("findByBudgetId")
    class FindByBudgetId {

        @Test
        @DisplayName("should return all payments for the budget")
        void shouldReturnAllPaymentsForBudget() {
            Payment payment = buildPayment(new BigDecimal("50.00"));
            when(paymentRepository.findByBudgetId(budgetId)).thenReturn(List.of(payment));

            List<Payment> result = paymentService.findByBudgetId(budgetId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBudgetId()).isEqualTo(budgetId);
        }

        @Test
        @DisplayName("should return empty list when no payments exist for budget")
        void shouldReturnEmptyListWhenNone() {
            when(paymentRepository.findByBudgetId(budgetId)).thenReturn(List.of());

            List<Payment> result = paymentService.findByBudgetId(budgetId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByPatientId")
    class FindByPatientId {

        @Test
        @DisplayName("should return all payments for the patient")
        void shouldReturnAllPaymentsForPatient() {
            Payment payment = buildPayment(new BigDecimal("75.00"));
            when(paymentRepository.findByPatientId(patientId)).thenReturn(List.of(payment));

            List<Payment> result = paymentService.findByPatientId(patientId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPatientId()).isEqualTo(patientId);
        }

        @Test
        @DisplayName("should return empty list when patient has no payments")
        void shouldReturnEmptyListWhenNone() {
            when(paymentRepository.findByPatientId(patientId)).thenReturn(List.of());

            List<Payment> result = paymentService.findByPatientId(patientId);

            assertThat(result).isEmpty();
        }
    }
}
