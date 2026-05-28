package com.adakadavra.dentis.api.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.adakadavra.dentis.api.security.entity.User;
import com.adakadavra.dentis.api.security.entity.UserRole;
import com.adakadavra.dentis.api.security.entity.UserStaffType;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'USER')")
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
    public ResponseEntity<ApiResponse<List<ClinicUserResponse>>> getUsers(
            @PathVariable UUID clinicId,
            @AuthenticationPrincipal User currentUser) {
        validateClinicAdminAccess(clinicId, currentUser);

        List<ClinicUserResponse> users = userRepository.findAllByClinicId(clinicId).stream()
                .map(this::toClinicUserResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @PostMapping("/{clinicId}/users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Create a user for a clinic")
    public ResponseEntity<ApiResponse<ClinicUserResponse>> createUser(
            @PathVariable UUID clinicId,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateClinicUserRequest request) {

        validateClinicAdminAccess(clinicId, currentUser);

        clinicUseCase.getClinic(clinicId);

        ResponseEntity<ApiResponse<ClinicUserResponse>> uniquenessError = validateUniqueUser(request.getUsername(), request.getEmail());
        if (uniquenessError != null) {
            return uniquenessError;
        }

        UserStaffType staffType = resolveStaffType(request.getStaffType());
        UserRole role = resolveClinicUserRole(request.getRole(), staffType);

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(role)
                .staffType(staffType)
                .clinicId(clinicId)
                .active(true)
                .build();

        User saved = userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(toClinicUserResponse(saved), "User created for clinic"));
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create a user globally [SUPER_ADMIN]")
    public ResponseEntity<ApiResponse<ClinicUserResponse>> createGlobalUser(
            @Valid @RequestBody CreateClinicUserRequest request) {

        UserStaffType staffType = resolveStaffType(request.getStaffType());
        UserRole role = resolveGlobalUserRole(request.getRole(), staffType);
        UUID targetClinicId = resolveTargetClinicId(request.getClinicId(), role);

        if (targetClinicId != null) {
            clinicUseCase.getClinic(targetClinicId);
        }

        ResponseEntity<ApiResponse<ClinicUserResponse>> uniquenessError = validateUniqueUser(request.getUsername(), request.getEmail());
        if (uniquenessError != null) {
            return uniquenessError;
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(role)
                .staffType(staffType)
                .clinicId(targetClinicId)
                .active(true)
                .build();

        User saved = userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(toClinicUserResponse(saved), "User created"));
    }

    @PutMapping("/{clinicId}/users/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update a clinic user")
    public ResponseEntity<ApiResponse<ClinicUserResponse>> updateUser(
            @PathVariable UUID clinicId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateClinicUserRequest request) {

        validateClinicAdminAccess(clinicId, currentUser);
        clinicUseCase.getClinic(clinicId);

        User user = getClinicUser(clinicId, userId);
        UserStaffType staffType = resolveStaffType(request.getStaffType());
        UserRole role = resolveClinicUserRole(request.getRole(), staffType);

        if (userRepository.existsByUsernameAndIdNot(request.getUsername(), userId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.ok(null, "Username already in use"));
        }

        if (userRepository.existsByEmailAndIdNot(request.getEmail(), userId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.ok(null, "Email already in use"));
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setRole(role);
        user.setStaffType(staffType);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User saved = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok(toClinicUserResponse(saved), "User updated"));
    }

    @DeleteMapping("/{clinicId}/users/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Deactivate a clinic user")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @PathVariable UUID clinicId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal User currentUser) {
        validateClinicAdminAccess(clinicId, currentUser);

        User user = getClinicUser(clinicId, userId);
        user.setActive(false);
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.ok(null, "User deactivated"));
    }

    private User getClinicUser(UUID clinicId, UUID userId) {
        return userRepository.findByIdAndClinicId(userId, clinicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clinic user not found"));
    }

    private void validateClinicAdminAccess(UUID clinicId, User currentUser) {
        if (currentUser == null) {
            return;
        }

        if (currentUser.getRole() == UserRole.ADMIN
                && currentUser.getClinicId() != null
                && !clinicId.equals(currentUser.getClinicId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot manage users from another clinic");
        }
    }

    private UserStaffType resolveStaffType(String staffType) {
        if (staffType == null || staffType.isBlank()) {
            return UserStaffType.ADMINISTRATIVE;
        }

        try {
            return UserStaffType.valueOf(staffType.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Staff type must be DENTIST or ADMINISTRATIVE");
        }
    }

    private UserRole resolveClinicUserRole(String role, UserStaffType staffType) {
        UserRole resolvedRole;
        if (role == null || role.isBlank()) {
            resolvedRole = UserRole.USER;
        } else {
            try {
                resolvedRole = UserRole.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be USER or ADMIN");
            }
        }

        if (resolvedRole == UserRole.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SUPER_ADMIN cannot belong to a clinic");
        }

        if (staffType == UserStaffType.DENTIST && resolvedRole != UserRole.USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dentists can only be created with USER role");
        }

        return resolvedRole;
    }

    private UserRole resolveGlobalUserRole(String role, UserStaffType staffType) {
        UserRole resolvedRole;
        if (role == null || role.isBlank()) {
            resolvedRole = UserRole.USER;
        } else {
            try {
                resolvedRole = UserRole.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be USER, ADMIN or SUPER_ADMIN");
            }
        }

        if (staffType == UserStaffType.DENTIST && resolvedRole != UserRole.USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dentists can only be created with USER role");
        }

        return resolvedRole;
    }

    private UUID resolveTargetClinicId(UUID clinicId, UserRole role) {
        if (role == UserRole.SUPER_ADMIN) {
            if (clinicId != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SUPER_ADMIN cannot belong to a clinic");
            }
            return null;
        }

        if (clinicId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Clinic is required when role is USER or ADMIN");
        }

        return clinicId;
    }

    private ResponseEntity<ApiResponse<ClinicUserResponse>> validateUniqueUser(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.ok(null, "Username already in use"));
        }

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.ok(null, "Email already in use"));
        }

        return null;
    }

    private ClinicUserResponse toClinicUserResponse(User user) {
        return ClinicUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .staffType(user.getStaffType().name())
                .active(user.isActive())
                .build();
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

        @Pattern(regexp = "^(USER|ADMIN|SUPER_ADMIN)$", message = "Role must be USER, ADMIN or SUPER_ADMIN")
        private final String role;

        @Pattern(regexp = "^(DENTIST|ADMINISTRATIVE)$", message = "Staff type must be DENTIST or ADMINISTRATIVE")
        private final String staffType;

        private final UUID clinicId;

        @JsonCreator
        public CreateClinicUserRequest(
                @JsonProperty("username") String username,
                @JsonProperty("email") String email,
                @JsonProperty("password") String password,
                @JsonProperty("fullName") String fullName,
                @JsonProperty("role") String role,
                @JsonProperty("staffType") String staffType,
                @JsonProperty("clinicId") UUID clinicId) {
            this.username = username;
            this.email = email;
            this.password = password;
            this.fullName = fullName;
            this.role = role;
            this.staffType = staffType;
            this.clinicId = clinicId;
        }
    }

    @Getter
    @Builder
    public static class UpdateClinicUserRequest {

        @NotBlank
        @Size(max = 100)
        private final String username;

        @NotBlank
        @Email
        @Size(max = 150)
        private final String email;

        @Size(min = 8, max = 100)
        private final String password;

        @NotBlank
        @Size(max = 200)
        private final String fullName;

        @Pattern(regexp = "^(USER|ADMIN)$", message = "Role must be USER or ADMIN")
        private final String role;

        @Pattern(regexp = "^(DENTIST|ADMINISTRATIVE)$", message = "Staff type must be DENTIST or ADMINISTRATIVE")
        private final String staffType;

        @JsonCreator
        public UpdateClinicUserRequest(
                @JsonProperty("username") String username,
                @JsonProperty("email") String email,
                @JsonProperty("password") String password,
                @JsonProperty("fullName") String fullName,
                @JsonProperty("role") String role,
                @JsonProperty("staffType") String staffType) {
            this.username = username;
            this.email = email;
            this.password = password;
            this.fullName = fullName;
            this.role = role;
            this.staffType = staffType;
        }
    }
}

