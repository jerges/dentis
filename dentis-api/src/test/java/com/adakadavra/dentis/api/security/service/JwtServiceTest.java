package com.adakadavra.dentis.api.security.service;

import com.adakadavra.dentis.api.security.entity.User;
import com.adakadavra.dentis.api.security.entity.UserRole;
import com.adakadavra.dentis.api.security.entity.UserStaffType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService")
class JwtServiceTest {

    private static final String TEST_SECRET =
            "dentis-test-secret-key-minimum-256-bits-long-for-hs256-algorithm";
    private static final long EXPIRATION_MS = 86_400_000L;

    @Mock private Environment environment;

    private JwtService jwtService;
    private User dentistUser;
    private User superAdminUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(environment);
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", EXPIRATION_MS);

        UUID clinicId = UUID.randomUUID();
        dentistUser = User.builder()
                .id(UUID.randomUUID())
                .username("dra.garcia")
                .email("garcia@clinica.com")
                .password("hashed")
                .role(UserRole.USER)
                .staffType(UserStaffType.DENTIST)
                .clinicId(clinicId)
                .fullName("Dra. María García")
                .active(true)
                .build();

        superAdminUser = User.builder()
                .id(UUID.randomUUID())
                .username("jbello")
                .email("jbello@dentis.com")
                .password("hashed")
                .role(UserRole.SUPER_ADMIN)
                .staffType(UserStaffType.ADMINISTRATIVE)
                .clinicId(null)
                .fullName("Jerges Bello")
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("validateSecret")
    class ValidateSecret {

        @Test
        @DisplayName("should throw when insecure default secret is used outside dev profile")
        void shouldThrowWhenInsecureSecretUsedOutsideDev() {
            JwtService insecureService = new JwtService(environment);
            ReflectionTestUtils.setField(insecureService, "secret",
                    "dentis-jwt-secret-key-minimum-256-bits-long-for-hs256");
            ReflectionTestUtils.setField(insecureService, "expirationMs", EXPIRATION_MS);
            when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

            assertThatThrownBy(insecureService::validateSecret)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("JWT_SECRET");
        }

        @Test
        @DisplayName("should not throw when insecure secret is used in dev profile")
        void shouldNotThrowInDevProfile() {
            JwtService devService = new JwtService(environment);
            ReflectionTestUtils.setField(devService, "secret",
                    "dentis-jwt-secret-key-minimum-256-bits-long-for-hs256");
            ReflectionTestUtils.setField(devService, "expirationMs", EXPIRATION_MS);
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

            devService.validateSecret();
        }
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("should generate a non-blank token for a dentist user")
        void shouldGenerateNonBlankTokenForDentist() {
            String token = jwtService.generateToken(dentistUser);

            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("should embed role in token claims")
        void shouldEmbedRoleInToken() {
            String token = jwtService.generateToken(dentistUser);

            assertThat(jwtService.extractRole(token)).isEqualTo("USER");
        }

        @Test
        @DisplayName("should embed clinicId in token claims for clinic users")
        void shouldEmbedClinicIdForClinicUsers() {
            String token = jwtService.generateToken(dentistUser);

            assertThat(jwtService.extractClinicId(token))
                    .isEqualTo(dentistUser.getClinicId().toString());
        }

        @Test
        @DisplayName("should not include clinicId claim for SUPER_ADMIN")
        void shouldNotIncludeClinicIdForSuperAdmin() {
            String token = jwtService.generateToken(superAdminUser);

            assertThat(jwtService.extractClinicId(token)).isNull();
        }

        @Test
        @DisplayName("should embed SUPER_ADMIN role in token")
        void shouldEmbedSuperAdminRole() {
            String token = jwtService.generateToken(superAdminUser);

            assertThat(jwtService.extractRole(token)).isEqualTo("SUPER_ADMIN");
        }
    }

    @Nested
    @DisplayName("extractUsername")
    class ExtractUsername {

        @Test
        @DisplayName("should return the username used when generating the token")
        void shouldReturnCorrectUsername() {
            String token = jwtService.generateToken(dentistUser);

            assertThat(jwtService.extractUsername(token)).isEqualTo("dra.garcia");
        }
    }

    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValid {

        @Test
        @DisplayName("should be valid when token was generated for the same user")
        void shouldBeValidForSameUser() {
            String token = jwtService.generateToken(dentistUser);

            assertThat(jwtService.isTokenValid(token, dentistUser)).isTrue();
        }

        @Test
        @DisplayName("should be invalid when token was generated for a different user")
        void shouldBeInvalidForDifferentUser() {
            String token = jwtService.generateToken(dentistUser);

            assertThat(jwtService.isTokenValid(token, superAdminUser)).isFalse();
        }

        @Test
        @DisplayName("should be invalid when token is expired")
        void shouldBeInvalidWhenExpired() {
            ReflectionTestUtils.setField(jwtService, "expirationMs", -1L);
            String expiredToken = jwtService.generateToken(dentistUser);

            assertThat(jwtService.isTokenValid(expiredToken, dentistUser)).isFalse();
        }
    }

    @Nested
    @DisplayName("getExpirationMs")
    class GetExpirationMs {

        @Test
        @DisplayName("should return the configured expiration value")
        void shouldReturnConfiguredExpiration() {
            assertThat(jwtService.getExpirationMs()).isEqualTo(EXPIRATION_MS);
        }
    }
}
