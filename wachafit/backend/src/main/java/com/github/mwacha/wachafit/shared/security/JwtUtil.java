package com.github.mwacha.wachafit.shared.security;

import com.github.mwacha.wachafit.account.Account;
import com.github.mwacha.wachafit.user.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private static final long SELECT_TENANT_TOKEN_TTL_SECONDS = 5 * 60;

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtUtil(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.expiration}") long expirationSeconds
    ) {
        if (secret.length() < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(User user) {
        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("role", user.getRole().name())
            .claim("tenantId", user.getTenant().getId().toString())
            .claim("accountId", user.getAccount().getId().toString())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationSeconds * 1000))
            .signWith(key)
            .compact();
    }

    public String generateSelectTenantToken(Account account) {
        return Jwts.builder()
            .subject(account.getId().toString())
            .claim("purpose", "select-tenant")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + SELECT_TENANT_TOKEN_TTL_SECONDS * 1000))
            .signWith(key)
            .compact();
    }

    public boolean isSelectTenantToken(String token) {
        try {
            return "select-tenant".equals(parseClaims(token).get("purpose", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public UUID extractTenantId(String token) {
        String raw = parseClaims(token).get("tenantId", String.class);
        return raw != null ? UUID.fromString(raw) : null;
    }

    public UUID extractAccountId(String token) {
        String raw = parseClaims(token).get("accountId", String.class);
        return raw != null ? UUID.fromString(raw) : null;
    }

    public boolean isTokenValid(String token) {
        try {
            String subject = parseClaims(token).getSubject();
            UUID.fromString(subject); // validate subject is a UUID
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
