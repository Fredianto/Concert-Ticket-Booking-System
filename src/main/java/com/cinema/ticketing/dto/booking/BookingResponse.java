package com.cinema.ticketing.dto.booking;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        String bookingCode,
        String status,
        OffsetDateTime holdExpiresAt,
        BigDecimal totalAmount
) {
}
