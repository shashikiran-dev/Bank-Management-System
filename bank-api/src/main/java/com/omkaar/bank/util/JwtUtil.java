package com.omkaar.bank.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expiryMs;

    public JwtUtil(
            @Value("${bank.jwt.secret}") String secret,
            @Value("${bank.jwt.expiry-hours:2}") long expiryHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryMs = expiryHours * 60 * 60 * 1000;
    }

    /** Issue a signed token for a user. */
    public String generate(String userId, String email, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(userId)
                .addClaims(Map.of("email", email, "role", role))
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expiryMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Validate token and return its claims. Throws JwtException on failure. */
    public Claims validate(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** Extract userId (subject) from a raw token string — no validation. */
    public String extractUserId(String token) {
        return validate(token).getSubject();
    }

    /** Extract role claim from token. */
    public String extractRole(String token) {
        return (String) validate(token).get("role");
    }
}
