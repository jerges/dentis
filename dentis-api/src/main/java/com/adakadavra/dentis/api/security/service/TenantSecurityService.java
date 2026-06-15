package com.adakadavra.dentis.api.security.service;

import com.adakadavra.dentis.api.security.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Extracts the current authenticated user's clinic scope from the SecurityContext. */
@Component
public class TenantSecurityService {

    /** Returns the clinicId of the current user, or empty if the user is SUPER_ADMIN (unrestricted). */
    public Optional<UUID> currentClinicId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return Optional.ofNullable(user.getClinicId());
        }
        return Optional.empty();
    }
}
