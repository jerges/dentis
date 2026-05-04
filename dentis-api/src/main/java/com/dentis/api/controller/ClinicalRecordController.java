package com.dentis.api.controller;

import com.dentis.application.dto.request.CreateClinicalEvolutionRequest;
import com.dentis.application.dto.request.UpdateOdontogramRequest;
import com.dentis.application.dto.response.ClinicalEvolutionResponse;
import com.dentis.application.dto.response.ClinicalRecordResponse;
import com.dentis.application.dto.response.OdontogramToothResponse;
import com.dentis.application.service.ClinicalRecordService;
import com.dentis.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clinical-records")
@RequiredArgsConstructor
@Tag(name = "Clinical Records", description = "Patient clinical history and odontogram")
public class ClinicalRecordController {

    private final ClinicalRecordService clinicalRecordService;

    @PostMapping("/patient/{patientId}/initialize")
    @Operation(summary = "Initialize clinical record for patient")
    public ResponseEntity<ApiResponse<ClinicalRecordResponse>> initialize(@PathVariable String patientId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(clinicalRecordService.initializeForPatient(patientId)));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get patient clinical record")
    public ResponseEntity<ApiResponse<ClinicalRecordResponse>> findByPatient(@PathVariable String patientId) {
        return ResponseEntity.ok(ApiResponse.ok(clinicalRecordService.findByPatient(patientId)));
    }

    @PutMapping("/patient/{patientId}/odontogram")
    @Operation(summary = "Update tooth status in odontogram")
    public ResponseEntity<ApiResponse<OdontogramToothResponse>> updateTooth(
            @PathVariable String patientId,
            @Valid @RequestBody UpdateOdontogramRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(clinicalRecordService.updateTooth(patientId, request)));
    }

    @PostMapping("/evolutions")
    @Operation(summary = "Add clinical evolution note")
    public ResponseEntity<ApiResponse<ClinicalEvolutionResponse>> addEvolution(
            @Valid @RequestBody CreateClinicalEvolutionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(clinicalRecordService.addEvolution(request)));
    }
}
