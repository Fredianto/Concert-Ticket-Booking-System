package com.cinema.ticketing.dto.booking;

import java.math.BigDecimal;

public record CancelBookingRequest(
        String reason,
        BigDecimal refundAmount
) {
}
