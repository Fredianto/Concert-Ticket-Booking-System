package com.cinema.ticketing.dto.booking;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BookingCreateRequest(
        @NotNull UUID userId,
        @NotNull UUID concertId,
        @NotBlank String categoryCode,
        @NotNull @Min(1) Integer quantity,
        @NotBlank String idempotencyKey,
        String correlationId,
        Integer holdMinutes
) {
}
