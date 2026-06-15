package com.adakadavra.dentis.api.controller;

import com.adakadavra.dentis.api.security.service.TenantSecurityService;
import com.adakadavra.dentis.common.response.ApiResponse;
import com.adakadavra.dentis.common.response.PageResponse;
import com.adakadavra.dentis.patient.application.dto.CreatePatientRequest;
import com.adakadavra.dentis.patient.application.dto.PatientResponse;
import com.adakadavra.dentis.patient.application.dto.UpdatePatientRequest;
import com.adakadavra.dentis.patient.application.usecase.PatientUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Patient management operations")
public class PatientController {

    private final PatientUseCase patientUseCase;
    private final TenantSecurityService tenantSecurity;

    @PostMapping
    @Operation(summary = "Register a new patient")
    public ResponseEntity<ApiResponse<PatientResponse>> create(@Valid @RequestBody CreatePatientRequest request) {
        PatientResponse response = patientUseCase.createPatient(request, tenantSecurity.currentClinicId().orElse(null));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Patient registered successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient by ID")
    public ResponseEntity<ApiResponse<PatientResponse>> findById(@PathVariable UUID id) {
        Optional<UUID> clinicId = tenantSecurity.currentClinicId();
        PatientResponse response = clinicId.isPresent()
                ? patientUseCase.findByIdInClinic(id, clinicId.get())
                : patientUseCase.findById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    @Operation(summary = "List all patients")
    public ResponseEntity<ApiResponse<PageResponse<PatientResponse>>> findAll(
            @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
        Optional<UUID> clinicId = tenantSecurity.currentClinicId();
        Page<PatientResponse> page = clinicId.isPresent()
                ? patientUseCase.findAllByClinicId(clinicId.get(), pageable)
                : patientUseCase.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search patients by name")
    public ResponseEntity<ApiResponse<PageResponse<PatientResponse>>> search(
            @RequestParam String name,
            @PageableDefault(size = 20) Pageable pageable) {
        Optional<UUID> clinicId = tenantSecurity.currentClinicId();
        Page<PatientResponse> page = clinicId.isPresent()
                ? patientUseCase.searchByNameAndClinicId(name, clinicId.get(), pageable)
                : patientUseCase.searchByName(name, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update patient information")
    public ResponseEntity<ApiResponse<PatientResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePatientRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(patientUseCase.updatePatient(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a patient")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        patientUseCase.deactivatePatient(id);
        return ResponseEntity.noContent().build();
    }
}
