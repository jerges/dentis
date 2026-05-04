package com.dentis.application.service;

import com.dentis.application.dto.request.CreatePaymentRequest;
import com.dentis.application.dto.response.PaymentResponse;
import com.dentis.application.mapper.PaymentMapper;
import com.dentis.application.service.impl.PaymentServiceImpl;
import com.dentis.common.exception.BusinessException;
import com.dentis.common.exception.ResourceNotFoundException;
import com.dentis.domain.entity.Budget;
import com.dentis.domain.entity.BudgetItem;
import com.dentis.domain.entity.Patient;
import com.dentis.domain.entity.Payment;
import com.dentis.domain.enums.BudgetStatus;
import com.dentis.domain.enums.PaymentMethod;
import com.dentis.domain.enums.PaymentStatus;
import com.dentis.domain.repository.BudgetRepository;
import com.dentis.domain.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService")
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private BudgetRepository budgetRepository;
    @Mock private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Budget budget;
    private CreatePaymentRequest validRequest;

    @BeforeEach
    void setUp() {
        Patient patient = Patient.builder()
                .firstName("Luis")
                .lastName("Torres")
                .build();

        BudgetItem item = BudgetItem.builder()
                .description("Ortodoncia")
                .unitPrice(new BigDecimal("500.00"))
                .quantity(1)
                .discountPercentage(BigDecimal.ZERO)
                .build();

        budget = Budget.builder()
                .patient(patient)
                .status(BudgetStatus.APPROVED)
                .paymentStatus(PaymentStatus.PENDING)
                .items(new ArrayList<>(List.of(item)))
                .payments(new ArrayList<>())
                .build();

        validRequest = CreatePaymentRequest.builder()
                .budgetId("budget-1")
                .amount(new BigDecimal("200.00"))
                .paymentMethod(PaymentMethod.CASH)
                .paymentDate(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("should register partial payment and update budget status")
        void shouldRegisterPartialPayment() {
            when(budgetRepository.findById("budget-1")).thenReturn(Optional.of(budget));
            Payment payment = Payment.builder()
                    .budget(budget)
                    .amount(new BigDecimal("200.00"))
                    .paymentMethod(PaymentMethod.CASH)
                    .paymentDate(LocalDateTime.now())
                    .build();
            when(paymentRepository.save(any())).thenReturn(payment);
            when(paymentRepository.sumAmountByBudgetId("budget-1")).thenReturn(new BigDecimal("200.00"));
            when(budgetRepository.save(budget)).thenReturn(budget);
            PaymentResponse response = PaymentResponse.builder()
                    .amount(new BigDecimal("200.00"))
                    .paymentMethod(PaymentMethod.CASH)
                    .build();
            when(paymentMapper.toResponse(payment)).thenReturn(response);

            PaymentResponse result = paymentService.register(validRequest);

            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualByComparingTo("200.00");
            assertThat(budget.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIAL);
        }

        @Test
        @DisplayName("should mark budget as PAID when payment covers full amount")
        void shouldMarkBudgetAsPaidWhenFullyCovered() {
            when(budgetRepository.findById("budget-1")).thenReturn(Optional.of(budget));
            Payment payment = Payment.builder()
                    .budget(budget)
                    .amount(new BigDecimal("500.00"))
                    .paymentMethod(PaymentMethod.CASH)
                    .paymentDate(LocalDateTime.now())
                    .build();
            when(paymentRepository.save(any())).thenReturn(payment);
            when(paymentRepository.sumAmountByBudgetId("budget-1")).thenReturn(new BigDecimal("500.00"));
            when(budgetRepository.save(budget)).thenReturn(budget);
            when(paymentMapper.toResponse(payment)).thenReturn(
                    PaymentResponse.builder().amount(new BigDecimal("500.00")).build());

            validRequest.setAmount(new BigDecimal("500.00"));
            paymentService.register(validRequest);

            assertThat(budget.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        }

        @Test
        @DisplayName("should throw when budget is rejected")
        void shouldThrowWhenBudgetIsRejected() {
            budget.setStatus(BudgetStatus.REJECTED);
            when(budgetRepository.findById("budget-1")).thenReturn(Optional.of(budget));

            assertThatThrownBy(() -> paymentService.register(validRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("rejected or expired");
        }

        @Test
        @DisplayName("should throw when budget not found")
        void shouldThrowWhenBudgetNotFound() {
            when(budgetRepository.findById("bad-id")).thenReturn(Optional.empty());
            validRequest.setBudgetId("bad-id");

            assertThatThrownBy(() -> paymentService.register(validRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findByBudget()")
    class FindByBudget {

        @Test
        @DisplayName("should return payment list for budget")
        void shouldReturnPayments() {
            Payment p1 = Payment.builder().budget(budget).amount(new BigDecimal("100")).build();
            Payment p2 = Payment.builder().budget(budget).amount(new BigDecimal("150")).build();
            when(paymentRepository.findByBudgetId("budget-1")).thenReturn(List.of(p1, p2));
            when(paymentMapper.toResponse(p1)).thenReturn(PaymentResponse.builder().amount(new BigDecimal("100")).build());
            when(paymentMapper.toResponse(p2)).thenReturn(PaymentResponse.builder().amount(new BigDecimal("150")).build());

            List<PaymentResponse> result = paymentService.findByBudget("budget-1");

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getAmount()).isEqualByComparingTo("100");
        }
    }
}
