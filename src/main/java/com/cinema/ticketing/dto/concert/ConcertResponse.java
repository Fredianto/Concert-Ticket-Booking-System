package com.cinema.ticketing.dto.concert;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ConcertResponse(
        UUID id,
        String name,
        String artist,
        String venue,
        OffsetDateTime startsAt,
        String timezone,
        String status,
        BigDecimal basePrice,
        Integer capacity
) {
}
