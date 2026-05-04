package com.dentis.api.controller;

import com.dentis.application.dto.request.CreateBudgetRequest;
import com.dentis.application.dto.response.BudgetResponse;
import com.dentis.application.service.BudgetService;
import com.dentis.common.response.ApiResponse;
import com.dentis.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
@Tag(name = "Budgets", description = "Budget / presupuesto management")
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    @Operation(summary = "Create a budget for a patient")
    public ResponseEntity<ApiResponse<BudgetResponse>> create(
            @Valid @RequestBody CreateBudgetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(budgetService.create(request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get budget by ID")
    public ResponseEntity<ApiResponse<BudgetResponse>> findById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(budgetService.findById(id)));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get budgets by patient")
    public ResponseEntity<ApiResponse<PageResponse<BudgetResponse>>> findByPatient(
            @PathVariable String patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                budgetService.findByPatient(patientId, PageRequest.of(page, size))));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a budget")
    public ResponseEntity<ApiResponse<BudgetResponse>> approve(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(budgetService.approve(id)));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a budget")
    public ResponseEntity<ApiResponse<BudgetResponse>> reject(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(budgetService.reject(id)));
    }

    @PatchMapping("/{budgetId}/items/{itemId}/performed")
    @Operation(summary = "Mark a budget item as performed")
    public ResponseEntity<ApiResponse<Void>> markItemPerformed(
            @PathVariable String budgetId,
            @PathVariable String itemId) {
        budgetService.markItemPerformed(budgetId, itemId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Item marked as performed"));
    }
}
