package com.dentis.api.controller;

import com.dentis.application.dto.request.CreateAppointmentRequest;
import com.dentis.application.dto.request.UpdateAppointmentRequest;
import com.dentis.application.dto.response.AppointmentResponse;
import com.dentis.application.service.AppointmentService;
import com.dentis.common.response.ApiResponse;
import com.dentis.common.response.PageResponse;
import com.dentis.domain.enums.AppointmentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Clinical agenda management")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @Operation(summary = "Schedule a new appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> create(
            @Valid @RequestBody CreateAppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(appointmentService.create(request), "Appointment scheduled successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateAppointmentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.update(id, request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by ID")
    public ResponseEntity<ApiResponse<AppointmentResponse>> findById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.findById(id)));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get appointments by patient")
    public ResponseEntity<ApiResponse<PageResponse<AppointmentResponse>>> findByPatient(
            @PathVariable String patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                appointmentService.findByPatient(patientId, PageRequest.of(page, size))));
    }

    @GetMapping("/dentist/{dentistId}/calendar")
    @Operation(summary = "Get dentist calendar view between date range")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getCalendar(
            @PathVariable String dentistId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.findDentistCalendar(dentistId, from, to)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update appointment status")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateStatus(
            @PathVariable String id,
            @RequestParam AppointmentStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.updateStatus(id, status)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel appointment")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable String id) {
        appointmentService.cancel(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Appointment cancelled"));
    }
}
