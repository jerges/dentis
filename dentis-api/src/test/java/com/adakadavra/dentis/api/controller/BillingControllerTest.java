package com.adakadavra.dentis.api.controller;

import com.adakadavra.dentis.billing.domain.model.Budget;
import com.adakadavra.dentis.billing.domain.model.BudgetStatus;
import com.adakadavra.dentis.billing.domain.model.Payment;
import com.adakadavra.dentis.billing.domain.model.PaymentMethod;
import com.adakadavra.dentis.billing.domain.model.Tariff;
import com.adakadavra.dentis.billing.domain.model.TariffCategory;
import com.adakadavra.dentis.billing.domain.service.BudgetService;
import com.adakadavra.dentis.billing.domain.service.PaymentService;
import com.adakadavra.dentis.billing.domain.service.TariffService;
import com.adakadavra.dentis.common.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingController")
class BillingControllerTest {

    @Mock
    private BudgetService budgetService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private TariffService tariffService;

    @InjectMocks
    private BillingController billingController;

    @Test
    @DisplayName("should be returned with OK status when listing tariffs")
    void shouldBeReturnedWithOkStatusWhenListingTariffs() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        Page<Tariff> tariffsPage = new PageImpl<>(List.of(buildTariff("CONS-001", "Consultation")), pageable, 1);
        when(tariffService.findAll(pageable)).thenReturn(tariffsPage);

        // Act
        ResponseEntity<ApiResponse<Page<Tariff>>> response = billingController.getTariffs(pageable);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isEqualTo(tariffsPage);
    }

    @Test
    @DisplayName("should be created when budget is registered")
    void shouldBeCreatedWhenBudgetIsRegistered() {
        // Arrange
        Budget budget = Budget.builder()
                .id(UUID.randomUUID())
                .patientId(UUID.randomUUID())
                .dentistId(UUID.randomUUID())
                .items(Collections.emptyList())
                .status(BudgetStatus.DRAFT)
                .build();
        when(budgetService.createBudget(budget)).thenReturn(budget);

        // Act
        ResponseEntity<ApiResponse<Budget>> response = billingController.createBudget(budget);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("Budget created successfully");
        assertThat(response.getBody().getData()).isEqualTo(budget);
        verify(budgetService).createBudget(budget);
    }

    @Nested
    @DisplayName("createTariff")
    class CreateTariff {

        @Test
        @DisplayName("should be created with normalized values")
        void shouldBeCreatedWithNormalizedValues() {
            // Arrange
            BillingController.CreateTariffRequest request = new BillingController.CreateTariffRequest();
            request.setCode("  trf-001 ");
            request.setName("  General consultation  ");
            request.setDescription("Base consultation tariff");
            request.setCategory(TariffCategory.OTHER);
            request.setBasePrice(new BigDecimal("35.50"));
            request.setDiscountAllowed(null);

            Tariff createdTariff = buildTariff("TRF-001", "General consultation");
            createdTariff.setDiscountAllowed(false);
            when(tariffService.createTariff(org.mockito.ArgumentMatchers.any(Tariff.class))).thenReturn(createdTariff);

            // Act
            ResponseEntity<ApiResponse<Tariff>> response = billingController.createTariff(request);

            // Assert
            ArgumentCaptor<Tariff> tariffCaptor = ArgumentCaptor.forClass(Tariff.class);
            verify(tariffService).createTariff(tariffCaptor.capture());

            Tariff sentTariff = tariffCaptor.getValue();
            assertThat(sentTariff.getCode()).isEqualTo("TRF-001");
            assertThat(sentTariff.getName()).isEqualTo("General consultation");
            assertThat(sentTariff.isDiscountAllowed()).isFalse();
            assertThat(sentTariff.isActive()).isTrue();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getMessage()).isEqualTo("Tariff created successfully");
            assertThat(response.getBody().getData()).isEqualTo(createdTariff);
        }
    }

    @Test
    @DisplayName("should be no content when deactivating existing tariff")
    void shouldBeNoContentWhenDeactivatingExistingTariff() {
        // Arrange
        UUID tariffId = UUID.randomUUID();

        // Act
        ResponseEntity<Void> response = billingController.deactivateTariff(tariffId);

        // Assert
        verify(tariffService).deactivateTariff(tariffId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("should be created when payment is registered")
    void shouldBeCreatedWhenPaymentIsRegistered() {
        // Arrange
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .patientId(UUID.randomUUID())
                .budgetId(UUID.randomUUID())
                .amount(new BigDecimal("50.00"))
                .paymentMethod(PaymentMethod.CASH)
                .invoiceReference("INV-001")
                .build();
        when(paymentService.registerPayment(payment)).thenReturn(payment);

        // Act
        ResponseEntity<ApiResponse<Payment>> response = billingController.registerPayment(payment);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("Payment registered successfully");
        assertThat(response.getBody().getData()).isEqualTo(payment);
        verify(paymentService).registerPayment(payment);
    }

    private Tariff buildTariff(String code, String name) {
        return Tariff.builder()
                .id(UUID.randomUUID())
                .code(code)
                .name(name)
                .description(name)
                .category(TariffCategory.OTHER)
                .basePrice(new BigDecimal("25.00"))
                .discountAllowed(true)
                .active(true)
                .build();
    }
}

