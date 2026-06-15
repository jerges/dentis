package com.adakadavra.dentis.api.bdd.support;

import com.adakadavra.dentis.api.security.entity.User;
import com.adakadavra.dentis.api.security.entity.UserRole;
import com.adakadavra.dentis.api.security.entity.UserStaffType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Helper for injecting a real {@link User} entity into the MockMvc security context.
 * Required for {@link com.adakadavra.dentis.api.security.service.TenantSecurityService}
 * to resolve the current clinicId — generic role-based processors are not sufficient
 * because TenantSecurityService inspects {@code auth.getPrincipal() instanceof User}.
 */
public final class TenantAuthSupport {

    private TenantAuthSupport() {}

    /** Returns a processor that authenticates as an ADMIN user belonging to {@code clinicId}. */
    public static RequestPostProcessor asClinicAdmin(UUID clinicId) {
        User user = buildUser(UUID.randomUUID(), "clinic-admin-" + clinicId.toString().substring(0, 8),
                UserRole.ADMIN, clinicId);
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    /** Returns a processor that authenticates as a SUPER_ADMIN (no clinic restriction). */
    public static RequestPostProcessor asSuperAdmin() {
        User user = buildUser(UUID.randomUUID(), "super-admin-bdd", UserRole.SUPER_ADMIN, null);
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    /** Applies clinic-scoped authentication to an existing request builder. */
    public static MockHttpServletRequestBuilder withClinicAdmin(MockHttpServletRequestBuilder builder, UUID clinicId) {
        return builder.with(asClinicAdmin(clinicId));
    }

    /** Applies super-admin authentication to an existing request builder. */
    public static MockHttpServletRequestBuilder withSuperAdmin(MockHttpServletRequestBuilder builder) {
        return builder.with(asSuperAdmin());
    }

    private static User buildUser(UUID id, String username, UserRole role, UUID clinicId) {
        return User.builder()
                .id(id)
                .username(username)
                .email(username + "@bdd.test")
                .password("n/a")
                .role(role)
                .staffType(UserStaffType.ADMINISTRATIVE)
                .clinicId(clinicId)
                .fullName("BDD " + role.name())
                .active(true)
                .build();
    }
}
