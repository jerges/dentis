package com.adakadavra.dentis.billing.domain.service;

import com.adakadavra.dentis.billing.domain.model.*;
import com.adakadavra.dentis.billing.domain.repository.BudgetRepository;
import com.adakadavra.dentis.billing.domain.repository.PaymentRepository;
import com.adakadavra.dentis.billing.domain.repository.TariffRepository;
import com.adakadavra.dentis.common.exception.BusinessRuleException;
import com.adakadavra.dentis.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BudgetService")
class BudgetServiceTest {

    @Mock private BudgetRepository budgetRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private TariffRepository tariffRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BudgetService budgetService;

    private UUID tariffId;
    private Budget sampleBudget;

    @BeforeEach
    void setUp() {
        tariffId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        BudgetItem sampleItem = BudgetItem.builder()
                .id(UUID.randomUUID())
                .tariffId(tariffId)
                .description("Dental cleaning")
                .quantity(1)
                .unitPrice(new BigDecimal("50.00"))
                .discountPercentage(BigDecimal.ZERO)
                .paymentStatus(ProcedurePaymentStatus.PENDING)
                .build();

        sampleBudget = Budget.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .dentistId(UUID.randomUUID())
                .items(List.of(sampleItem))
                .status(BudgetStatus.DRAFT)
                .build();
    }

    @Nested
    @DisplayName("createBudget")
    class CreateBudget {

        @Test
        @DisplayName("should be created in DRAFT status")
        void shouldBeCreatedAsDraft() {
            Tariff tariff = Tariff.builder().id(tariffId).active(true).build();
            when(tariffRepository.findById(tariffId)).thenReturn(Optional.of(tariff));
            when(budgetRepository.save(any())).thenReturn(sampleBudget);

            Budget result = budgetService.createBudget(sampleBudget);

            assertThat(result.getStatus()).isEqualTo(BudgetStatus.DRAFT);
            verify(budgetRepository).save(any());
        }

        @Test
        @DisplayName("should be rejected when budget has no items")
        void shouldBeRejectedWhenBudgetIsEmpty() {
            Budget emptyBudget = sampleBudget.withItems(List.of());

            assertThatThrownBy(() -> budgetService.createBudget(emptyBudget))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("at least one item");
        }
    }

    @Nested
    @DisplayName("getBudgetSummary")
    class GetBudgetSummary {

        @Test
        @DisplayName("should be PAID when balance is zero")
        void shouldBePaidWhenBalanceIsZero() {
            UUID budgetId = sampleBudget.getId();
            when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(sampleBudget));
            when(paymentRepository.sumPaymentsByBudgetId(budgetId)).thenReturn(new BigDecimal("50.00"));

            BudgetSummary summary = budgetService.getBudgetSummary(budgetId);

            assertThat(summary.getStatus()).isEqualTo(PaymentSummaryStatus.PAID);
            assertThat(summary.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should be PARTIALLY_PAID when some amount is paid")
        void shouldBePartiallyPaidWhenSomeAmountIsPaid() {
            UUID budgetId = sampleBudget.getId();
            when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(sampleBudget));
            when(paymentRepository.sumPaymentsByBudgetId(budgetId)).thenReturn(new BigDecimal("25.00"));

            BudgetSummary summary = budgetService.getBudgetSummary(budgetId);

            assertThat(summary.getStatus()).isEqualTo(PaymentSummaryStatus.PARTIALLY_PAID);
            assertThat(summary.getBalance()).isEqualByComparingTo(new BigDecimal("25.00"));
        }

        @Test
        @DisplayName("should be PENDING when no payments are recorded")
        void shouldBePendingWhenNoPaymentsAreRecorded() {
            UUID budgetId = sampleBudget.getId();
            when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(sampleBudget));
            when(paymentRepository.sumPaymentsByBudgetId(budgetId)).thenReturn(BigDecimal.ZERO);

            BudgetSummary summary = budgetService.getBudgetSummary(budgetId);

            assertThat(summary.getStatus()).isEqualTo(PaymentSummaryStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("markItemAsPerformed")
    class MarkItemAsPerformed {

        @Test
        @DisplayName("should mark item as performed and set performedAt")
        void shouldMarkItemAsPerformed() {
            UUID itemId = sampleBudget.getItems().get(0).getId();
            when(budgetRepository.findById(sampleBudget.getId())).thenReturn(Optional.of(sampleBudget));
            when(budgetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Budget result = budgetService.markItemAsPerformed(sampleBudget.getId(), itemId);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getPerformed()).isTrue();
            assertThat(result.getItems().get(0).getPerformedAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw when item is not in budget")
        void shouldThrowWhenItemNotFound() {
            UUID unknownItemId = UUID.randomUUID();
            when(budgetRepository.findById(sampleBudget.getId())).thenReturn(Optional.of(sampleBudget));

            assertThatThrownBy(() -> budgetService.markItemAsPerformed(sampleBudget.getId(), unknownItemId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw when budget does not exist")
        void shouldThrowWhenBudgetNotFound() {
            UUID unknownBudgetId = UUID.randomUUID();
            when(budgetRepository.findById(unknownBudgetId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.markItemAsPerformed(unknownBudgetId, UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
