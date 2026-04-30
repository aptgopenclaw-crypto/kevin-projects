package com.taipei.iot.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

    private static final int MIN_SECRET_BYTES = 32; // 256 bits for HMAC-SHA256

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final long temporaryTokenExpiration;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration,
            @Value("${jwt.temporary-token-expiration}") long temporaryTokenExpiration) {

        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes (256 bits). "
                    + "Set JWT_SECRET environment variable with a secure random value.");
        }

        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.temporaryTokenExpiration = temporaryTokenExpiration;
    }

    public String generateAccessToken(String userId, String email,
                                       String tenantId, List<String> roles,
                                       String deptId, List<String> permissions,
                                       String dataScope) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("tenantId", tenantId);
        claims.put("roles", roles);
        claims.put("deptId", deptId);
        claims.put("permissions", permissions);
        claims.put("dataScope", dataScope);

        return Jwts.builder()
                .claims(claims)
                .subject(userId)   // subject 統一使用 userId
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(key)
                .compact();
    }

    public String generateTemporaryToken(String userId, String email, boolean isSuperAdmin) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("tenantId", null);
        claims.put("roles", List.of());
        claims.put("temporary", true);
        claims.put("isSuperAdmin", isSuperAdmin);

        return Jwts.builder()
                .claims(claims)
                .subject(userId)   // subject 統一使用 userId
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + temporaryTokenExpiration))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String userId, String tenantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("type", "refresh");
        // 記錄發行時的 tenantId，refreshToken() 換新 token 時沿用，避免跨租戶漂移
        claims.put("tenantId", tenantId);

        return Jwts.builder()
                .claims(claims)
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
