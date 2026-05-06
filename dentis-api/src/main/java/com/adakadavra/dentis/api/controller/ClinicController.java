package com.adakadavra.dentis.api.controller;

import com.adakadavra.dentis.api.security.entity.User;
import com.adakadavra.dentis.api.security.entity.UserRole;
import com.adakadavra.dentis.api.security.repository.UserRepository;
import com.adakadavra.dentis.clinic.application.dto.ClinicResponse;
import com.adakadavra.dentis.clinic.application.dto.ClinicUserResponse;
import com.adakadavra.dentis.clinic.application.dto.CreateClinicRequest;
import com.adakadavra.dentis.clinic.application.dto.UpdateClinicRequest;
import com.adakadavra.dentis.clinic.application.usecase.ClinicUseCase;
import com.adakadavra.dentis.common.response.ApiResponse;
import com.adakadavra.dentis.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics")
@RequiredArgsConstructor
@Tag(name = "Clinics", description = "Clinic management — SUPER_ADMIN and ADMIN roles")
public class ClinicController {

    private final ClinicUseCase clinicUseCase;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Clinic CRUD ──────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create a new clinic [SUPER_ADMIN]")
    public ResponseEntity<ApiResponse<ClinicResponse>> create(
            @Valid @RequestBody CreateClinicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(clinicUseCase.createClinic(request), "Clinic created"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "List clinics with pagination")
    public ResponseEntity<ApiResponse<PageResponse<ClinicResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = clinicUseCase.getAllClinics(PageRequest.of(page, size, Sort.by("name")));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MEDICO')")
    @Operation(summary = "List all active clinics")
    public ResponseEntity<ApiResponse<List<ClinicResponse>>> getAllActive() {
        return ResponseEntity.ok(ApiResponse.ok(clinicUseCase.getAllActiveClinics()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Get clinic by id")
    public ResponseEntity<ApiResponse<ClinicResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(clinicUseCase.getClinic(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update clinic")
    public ResponseEntity<ApiResponse<ClinicResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClinicRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(clinicUseCase.updateClinic(id, request), "Clinic updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Deactivate a clinic [SUPER_ADMIN]")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        clinicUseCase.deactivateClinic(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Clinic deactivated"));
    }

    // ── Clinic Users ─────────────────────────────────────────────────────────

    @GetMapping("/{clinicId}/users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "List users of a clinic")
    public ResponseEntity<ApiResponse<List<ClinicUserResponse>>> getUsers(@PathVariable UUID clinicId) {
        List<ClinicUserResponse> users = userRepository.findAllByClinicId(clinicId).stream()
                .map(u -> ClinicUserResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .email(u.getEmail())
                        .fullName(u.getFullName())
                        .role(u.getRole().name())
                        .active(u.isActive())
                        .build())
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @PostMapping("/{clinicId}/users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Create a user for a clinic")
    public ResponseEntity<ApiResponse<ClinicUserResponse>> createUser(
            @PathVariable UUID clinicId,
            @Valid @RequestBody CreateClinicUserRequest request) {

        clinicUseCase.getClinic(clinicId);

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.ok(null, "Email already in use"));
        }

        UserRole role = request.getRole() != null
                ? UserRole.valueOf(request.getRole().toUpperCase())
                : UserRole.MEDICO;

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(role)
                .clinicId(clinicId)
                .active(true)
                .build();

        User saved = userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                ClinicUserResponse.builder()
                        .id(saved.getId())
                        .username(saved.getUsername())
                        .email(saved.getEmail())
                        .fullName(saved.getFullName())
                        .role(saved.getRole().name())
                        .active(saved.isActive())
                        .build(),
                "User created for clinic"));
    }

    @DeleteMapping("/{clinicId}/users/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Deactivate a clinic user")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @PathVariable UUID clinicId,
            @PathVariable UUID userId) {
        userRepository.findById(userId).ifPresent(u -> {
            if (clinicId.equals(u.getClinicId())) {
                u.setActive(false);
                userRepository.save(u);
            }
        });
        return ResponseEntity.ok(ApiResponse.ok(null, "User deactivated"));
    }

    // ── Inner DTOs ────────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class CreateClinicUserRequest {

        @NotBlank
        @Size(max = 100)
        private final String username;

        @NotBlank
        @Email
        @Size(max = 150)
        private final String email;

        @NotBlank
        @Size(min = 8, max = 100)
        private final String password;

        @NotBlank
        @Size(max = 200)
        private final String fullName;

        @Pattern(regexp = "^(ADMIN|MEDICO)$", message = "Role must be ADMIN or MEDICO")
        private final String role;
    }
}

