package com.cinema.ticketing.service;

import com.cinema.ticketing.config.AuthProperties;
import com.cinema.ticketing.dto.auth.AuthRequest;
import com.cinema.ticketing.dto.auth.AuthResponse;
import com.cinema.ticketing.security.JwtService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final AuthProperties authProperties;
    private final JwtService jwtService;
    private final JdbcTemplate jdbcTemplate;

    public AuthService(AuthProperties authProperties, JwtService jwtService, JdbcTemplate jdbcTemplate) {
        this.authProperties = authProperties;
        this.jwtService = jwtService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public AuthResponse login(AuthRequest request) {
        return authProperties.getUsers().stream()
                .filter(u -> u.getUsername().equals(request.username()) && u.getPassword().equals(request.password()))
                .findFirst()
                .map(u -> {
                    String token = jwtService.generateToken(u.getUsername(), u.getRole());
                    UUID userId = ensureAppUser(u.getUsername(), u.getRole());
                    return new AuthResponse(token, "Bearer", authProperties.getJwt().getExpirationMinutes() * 60, u.getRole(), userId);
                })
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
    }

    private UUID ensureAppUser(String username, String role) {
        String email = username + "@local.ticketing";

        String existingSql = "SELECT id FROM ticketing.app_user WHERE email = ?";
        UUID existingId = jdbcTemplate.query(existingSql, (rs, rn) -> UUID.fromString(rs.getString("id")), email)
                .stream()
                .findFirst()
                .orElse(null);

        if (existingId != null) {
            jdbcTemplate.update(
                    "UPDATE ticketing.app_user SET role = ?::ticketing.user_role, full_name = ? WHERE id = ?",
                    role.toUpperCase(),
                    username,
                    existingId);
            return existingId;
        }

        UUID newId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO ticketing.app_user(id, email, full_name, role) VALUES (?, ?, ?, ?::ticketing.user_role)",
                newId,
                email,
                username,
                role.toUpperCase());
        return newId;
    }
}
