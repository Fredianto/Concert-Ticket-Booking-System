package com.cinema.ticketing.dto.report;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionRow(
        UUID id,
        UUID bookingId,
        String method,
        BigDecimal amount,
        String status,
        String providerRef,
        OffsetDateTime createdAt,
        OffsetDateTime paidAt
) {
}
