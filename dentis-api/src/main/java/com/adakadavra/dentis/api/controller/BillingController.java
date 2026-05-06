package com.adakadavra.dentis.api.controller;

import com.adakadavra.dentis.billing.domain.model.*;
import com.adakadavra.dentis.billing.domain.service.BudgetService;
import com.adakadavra.dentis.billing.domain.service.PaymentService;
import com.adakadavra.dentis.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
