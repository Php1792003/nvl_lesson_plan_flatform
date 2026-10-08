package com.school.lessonplan.security;

import com.school.lessonplan.config.AppProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final AppProperties appProperties;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.key = Keys.hmacShaKeyFor(
            appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Tạo access token (15 phút)
     */
    public String generateAccessToken(Long userId, String email, int tokenVersion) {
        Instant now = Instant.now();
        Instant expiry = now.plus(appProperties.getJwt().getAccessTokenExpiryMinutes(), ChronoUnit.MINUTES);

        return Jwts.builder()
            .subject(String.valueOf(userId))
            .id(UUID.randomUUID().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .claim("email", email)
            .claim("tv", tokenVersion)
            .signWith(key, Jwts.SIG.HS256)
            .compact();
    }

    /**
     * Tạo refresh token (7 ngày) - trả về raw token, caller sẽ hash lưu DB
     */
    public String generateRefreshTokenValue() {
        return UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
    }

    /**
     * Parse và validate access token
     */
    public Jws<Claims> parseAccessToken(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token);
    }

    /**
     * Lấy user id từ token
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseAccessToken(token).getPayload();
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Lấy token version từ token
     */
    public int getTokenVersionFromToken(String token) {
        Claims claims = parseAccessToken(token).getPayload();
        return claims.get("tv", Integer.class);
    }

    /**
     * Tính thời điểm hết hạn refresh token
     */
    public Instant getRefreshTokenExpiry() {
        return Instant.now().plus(appProperties.getJwt().getRefreshTokenExpiryDays(), ChronoUnit.DAYS);
    }
}
