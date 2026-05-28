package com.adakadavra.dentis.api.controller;

import com.adakadavra.dentis.api.security.service.JwtService;
import com.adakadavra.dentis.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import com.adakadavra.dentis.api.security.entity.User;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Profile("!dev")
@Tag(name = "Authentication", description = "JWT authentication endpoints")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate and get JWT token")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("USER");
        return ResponseEntity.ok(ApiResponse.ok(TokenResponse.builder()
                .token(token)
                .username(userDetails.getUsername())
                .role(role)
                .clinicId(userDetails instanceof User user && user.getClinicId() != null ? user.getClinicId().toString() : null)
                .expiresIn(jwtService.getExpirationMs())
                .build()));
    }

    @Getter
    public static class LoginRequest {
        @NotBlank private final String username;
        @NotBlank private final String password;
        @com.fasterxml.jackson.annotation.JsonCreator
        public LoginRequest(
                @com.fasterxml.jackson.annotation.JsonProperty("username") String username,
                @com.fasterxml.jackson.annotation.JsonProperty("password") String password) {
            this.username = username;
            this.password = password;
        }
    }

    @Getter
    @Builder
    public static class TokenResponse {
        private final String token;
        private final String type = "Bearer";
        private final String username;
        private final String role;
        private final String clinicId;
        private final long expiresIn;
    }
}
