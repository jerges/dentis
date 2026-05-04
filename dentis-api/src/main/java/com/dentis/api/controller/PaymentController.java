package com.dentis.api.controller;

import com.dentis.application.dto.request.CreatePaymentRequest;
import com.dentis.application.dto.response.PaymentResponse;
import com.dentis.application.service.PaymentService;
import com.dentis.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment registration and tracking")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Register a payment for a budget")
    public ResponseEntity<ApiResponse<PaymentResponse>> register(
            @Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(paymentService.register(request), "Payment registered successfully"));
    }

    @GetMapping("/budget/{budgetId}")
    @Operation(summary = "Get all payments for a budget")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> findByBudget(
            @PathVariable String budgetId) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.findByBudget(budgetId)));
    }
}
