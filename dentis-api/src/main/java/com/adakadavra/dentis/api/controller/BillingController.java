package com.adakadavra.dentis.api.controller;

import com.adakadavra.dentis.billing.domain.model.*;
import com.adakadavra.dentis.billing.domain.service.BudgetService;
import com.adakadavra.dentis.billing.domain.service.PaymentService;
import com.adakadavra.dentis.billing.domain.service.TariffService;
import com.adakadavra.dentis.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@Tag(name = "Billing", description = "Budget and payment management")
public class BillingController {

    private final BudgetService budgetService;
    private final PaymentService paymentService;
    private final TariffService tariffService;

    @GetMapping("/tariffs")
    @Operation(summary = "List tariffs")
    public ResponseEntity<ApiResponse<Page<Tariff>>> getTariffs(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(tariffService.findAll(pageable)));
    }

    @PostMapping("/tariffs")
    @Operation(summary = "Create a tariff")
    public ResponseEntity<ApiResponse<Tariff>> createTariff(@Valid @RequestBody CreateTariffRequest request) {
        Tariff tariff = Tariff.builder()
                .code(request.getCode().trim().toUpperCase())
                .name(request.getName().trim())
                .description(request.getDescription())
                .category(request.getCategory())
                .basePrice(request.getBasePrice())
                .discountAllowed(Boolean.TRUE.equals(request.getDiscountAllowed()))
                .active(true)
                .build();
        Tariff created = tariffService.createTariff(tariff);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created, "Tariff created successfully"));
    }

    @DeleteMapping("/tariffs/{id}")
    @Operation(summary = "Deactivate a tariff")
    public ResponseEntity<Void> deactivateTariff(@PathVariable UUID id) {
        tariffService.deactivateTariff(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/budgets")
    @Operation(summary = "Create a treatment budget")
    public ResponseEntity<ApiResponse<Budget>> createBudget(@Valid @RequestBody Budget budget) {
        Budget created = budgetService.createBudget(budget);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created, "Budget created successfully"));
    }

    @GetMapping("/budgets/{id}")
    @Operation(summary = "Get budget by ID")
    public ResponseEntity<ApiResponse<Budget>> getBudget(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(budgetService.findById(id)));
    }

    @PatchMapping("/budgets/{id}/approve")
    @Operation(summary = "Approve a budget")
    public ResponseEntity<ApiResponse<Budget>> approveBudget(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(budgetService.approveBudget(id)));
    }

    @GetMapping("/budgets/{id}/summary")
    @Operation(summary = "Get financial summary of a budget")
    public ResponseEntity<ApiResponse<BudgetSummary>> getBudgetSummary(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(budgetService.getBudgetSummary(id)));
    }

    @PostMapping("/payments")
    @Operation(summary = "Register a payment")
    public ResponseEntity<ApiResponse<Payment>> registerPayment(@Valid @RequestBody Payment payment) {
        Payment registered = paymentService.registerPayment(payment);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(registered, "Payment registered successfully"));
    }

    @GetMapping("/payments/budget/{budgetId}")
    @Operation(summary = "List payments for a budget")
    public ResponseEntity<ApiResponse<List<Payment>>> getPaymentsByBudget(@PathVariable UUID budgetId) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.findByBudgetId(budgetId)));
    }

    @GetMapping("/payments/patient/{patientId}")
    @Operation(summary = "List payments for a patient")
    public ResponseEntity<ApiResponse<List<Payment>>> getPaymentsByPatient(@PathVariable UUID patientId) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.findByPatientId(patientId)));
    }

    @GetMapping("/budgets/patient/{patientId}")
    @Operation(summary = "List budgets for a patient")
    public ResponseEntity<ApiResponse<Page<Budget>>> getBudgetsByPatient(
            @PathVariable UUID patientId, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(budgetService.findByPatientId(patientId, pageable)));
    }

    @lombok.Data
    public static class CreateTariffRequest {
        @NotBlank
        @Size(max = 30)
        private String code;

        @NotBlank
        @Size(max = 200)
        private String name;

        private String description;

        @NotNull
        private TariffCategory category;

        @NotNull
        @DecimalMin("0.01")
        private java.math.BigDecimal basePrice;

        private Boolean discountAllowed;
    }
}
