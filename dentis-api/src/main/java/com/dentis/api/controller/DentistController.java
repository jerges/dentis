package com.dentis.api.controller;

import com.dentis.application.dto.request.CreateDentistRequest;
import com.dentis.application.dto.response.DentistResponse;
import com.dentis.application.service.DentistService;
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
@RequestMapping("/api/v1/dentists")
@RequiredArgsConstructor
@Tag(name = "Dentists", description = "Dentist management")
public class DentistController {

    private final DentistService dentistService;

    @PostMapping
    @Operation(summary = "Create a new dentist")
    public ResponseEntity<ApiResponse<DentistResponse>> create(
            @Valid @RequestBody CreateDentistRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(dentistService.create(request), "Dentist created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dentist by ID")
    public ResponseEntity<ApiResponse<DentistResponse>> findById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(dentistService.findById(id)));
    }

    @GetMapping
    @Operation(summary = "List all active dentists")
    public ResponseEntity<ApiResponse<List<DentistResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(dentistService.findAllActive()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate dentist")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable String id) {
        dentistService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Dentist deactivated"));
    }
}
