package com.adakadavra.dentis.api.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.adakadavra.dentis.api.security.entity.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String INSECURE_DEFAULT = "dentis-jwt-secret-key-minimum-256-bits-long-for-hs256";

    @Value("${dentis.security.jwt.secret}")
    private String secret;

    @Value("${dentis.security.jwt.expiration-ms:86400000}")
    private long expirationMs;

    private final Environment environment;

    public JwtService(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validateSecret() {
        boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev");
        if (!isDev && INSECURE_DEFAULT.equals(secret)) {
            throw new IllegalStateException(
                "JWT_SECRET environment variable must be set to a secure value in non-dev environments. " +
                "The default insecure secret cannot be used in production."
            );
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        if (userDetails instanceof User user) {
            claims.put("role", user.getRole().name());
            claims.put("staffType", user.getStaffType().name());
            if (user.getClinicId() != null) {
                claims.put("clinicId", user.getClinicId().toString());
            }
        }
        return generateToken(claims, userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey())
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractClinicId(String token) {
        return extractClaim(token, claims -> claims.get("clinicId", String.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
