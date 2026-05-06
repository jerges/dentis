package com.adakadavra.dentis.api.security.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
/**
 * Security beans shared across all profiles (dev, prod, etc.).
 * Keeps PasswordEncoder available regardless of the active profile.
 */
@Configuration
public class SharedSecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
