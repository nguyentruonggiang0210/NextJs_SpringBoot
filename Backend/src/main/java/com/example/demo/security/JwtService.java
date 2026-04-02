package com.example.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(JwtProperties jwtProperties) {
        // HMAC-SHA256 yêu cầu key >= 32 bytes; pad nếu secret quá ngắn
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            keyBytes = Arrays.copyOf(keyBytes, 32);
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = jwtProperties.getExpirationMs();
    }

    /** Tạo JWT access token chứa userId, tên user và role (guest/member/admin). */
    public String generateToken(Long userId, String name, String role) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("name", name)
                .claim("role", role != null ? role : "guest")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /** Parse và trả về payload (Claims) của token. Ném exception nếu invalid/hết hạn. */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long extractUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }
}
