package com.cinema.ticketing.dto.concert;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ConcertUpsertRequest(
        @NotBlank String name,
        @NotBlank String artist,
        @NotBlank String venue,
        @NotNull @Future OffsetDateTime startsAt,
        @NotBlank String timezone,
        @NotBlank String status,
        @NotNull @Min(1) BigDecimal basePrice,
        @NotNull @Min(1) Integer capacity
) {
}
