package com.dentis.api.controller;

import com.dentis.application.dto.request.CreatePatientRequest;
import com.dentis.application.dto.request.UpdatePatientRequest;
import com.dentis.application.dto.response.PatientResponse;
import com.dentis.application.dto.response.PatientSummaryResponse;
import com.dentis.application.service.PatientService;
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
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Patient management")
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    @Operation(summary = "Create a new patient")
    public ResponseEntity<ApiResponse<PatientResponse>> create(
            @Valid @RequestBody CreatePatientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(patientService.create(request), "Patient created successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update patient")
    public ResponseEntity<ApiResponse<PatientResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdatePatientRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(patientService.update(id, request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient by ID")
    public ResponseEntity<ApiResponse<PatientResponse>> findById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(patientService.findById(id)));
    }

    @GetMapping
    @Operation(summary = "List all active patients")
    public ResponseEntity<ApiResponse<PageResponse<PatientSummaryResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(patientService.findAll(PageRequest.of(page, size))));
    }

    @GetMapping("/search")
    @Operation(summary = "Search patients by name, document or email")
    public ResponseEntity<ApiResponse<PageResponse<PatientSummaryResponse>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(patientService.search(q, PageRequest.of(page, size))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate patient")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable String id) {
        patientService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Patient deactivated"));
    }
}
