package com.cinema.ticketing.dto.concert;

public record AvailabilityResponse(
        String categoryCode,
        Integer totalStock,
        Integer soldStock,
        Integer availableStock
) {
}
