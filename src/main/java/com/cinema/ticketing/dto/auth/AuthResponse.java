package com.cinema.ticketing.dto.auth;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String role,
        UUID userId
) {
}
