package com.cinema.ticketing.security;

import com.cinema.ticketing.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final AuthProperties authProperties;

    public JwtService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public String generateToken(String username, String role) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(authProperties.getJwt().getExpirationMinutes() * 60);

        return Jwts.builder()
                .subject(username)
                .claims(Map.of("role", role))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(authProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
