package com.adakadavra.dentis.api.controller;

import com.adakadavra.dentis.common.response.ApiResponse;
import com.adakadavra.dentis.scheduling.application.dto.AppointmentResponse;
import com.adakadavra.dentis.scheduling.application.dto.CreateAppointmentRequest;
import com.adakadavra.dentis.scheduling.domain.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Appointment scheduling operations")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @Operation(summary = "Schedule a new appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> schedule(@Valid @RequestBody CreateAppointmentRequest request) {
        AppointmentResponse response = appointmentService.scheduleAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Appointment scheduled successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by ID")
    public ResponseEntity<ApiResponse<AppointmentResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.findById(id)));
    }

    @GetMapping("/dentist/{dentistId}")
    @Operation(summary = "Get dentist's appointments within a date range")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> findByDentist(
            @PathVariable UUID dentistId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.findByDentistAndDateRange(dentistId, from, to)));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get all appointments for a patient")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> findByPatient(@PathVariable UUID patientId) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.findByPatient(patientId)));
    }

    @PatchMapping("/{id}/confirm")
    @Operation(summary = "Confirm an appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.confirmAppointment(id)));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel an appointment")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        appointmentService.cancelAppointment(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reschedule")
    @Operation(summary = "Reschedule an appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> reschedule(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newEnd) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.rescheduleAppointment(id, newStart, newEnd)));
    }
}
