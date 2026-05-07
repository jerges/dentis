package com.adakadavra.dentis.api.controller;

import com.adakadavra.dentis.api.security.service.JwtService;
import com.adakadavra.dentis.common.response.ApiError;
import com.adakadavra.dentis.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Development-only auth endpoint mapped to the standard /api/v1/auth/login path.
 * Validates credentials against the real UserDetailsService (DB) + PasswordEncoder.
 * Activated only when the "dev" Spring profile is active.
 *
 * Dev superuser: jbello / Admin@2026!
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Profile("dev")
@Tag(name = "Authentication", description = "JWT authentication endpoints (dev mode)")
public class DevAuthController {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    @Operation(summary = "[DEV] Authenticate with real credentials and get JWT token")
    public ResponseEntity<ApiResponse<AuthController.TokenResponse>> login(
            @Valid @RequestBody AuthController.LoginRequest request) {

        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        } catch (UsernameNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(ApiError.builder()
                            .code("UNAUTHORIZED").message("Invalid username or password").build()));
        }

        if (!passwordEncoder.matches(request.getPassword(), userDetails.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(ApiError.builder()
                            .code("UNAUTHORIZED").message("Invalid username or password").build()));
        }

        String token = jwtService.generateToken(userDetails);
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("USER");
        return ResponseEntity.ok(ApiResponse.ok(
                AuthController.TokenResponse.builder()
                        .token(token)
                        .username(userDetails.getUsername())
                        .role(role)
                        .expiresIn(jwtService.getExpirationMs())
                        .build()));
    }
}
