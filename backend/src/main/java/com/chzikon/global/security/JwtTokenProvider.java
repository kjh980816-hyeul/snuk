package com.chzikon.global.security;

import com.chzikon.member.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessValiditySeconds;
    private final long refreshValiditySeconds;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-validity-seconds}") long accessValiditySeconds,
            @Value("${app.jwt.refresh-token-validity-seconds}") long refreshValiditySeconds) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be >= 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessValiditySeconds = accessValiditySeconds;
        this.refreshValiditySeconds = refreshValiditySeconds;
    }

    public String createAccessToken(Long memberId, Role role) {
        return build(memberId, role.name(), "access", accessValiditySeconds);
    }

    public String createRefreshToken(Long memberId, Role role) {
        return build(memberId, role.name(), "refresh", refreshValiditySeconds);
    }

    private String build(Long memberId, String role, String type, long validitySeconds) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + validitySeconds * 1000);
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("role", role)
                .claim("type", type)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return "access".equals(claims.get("type", String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("type", String.class));
    }

    public long getAccessValiditySeconds() {
        return accessValiditySeconds;
    }

    public long getRefreshValiditySeconds() {
        return refreshValiditySeconds;
    }
}
