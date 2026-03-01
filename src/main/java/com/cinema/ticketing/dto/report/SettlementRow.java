package com.cinema.ticketing.dto.report;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SettlementRow(
        UUID bookingId,
        String bookingCode,
        String userEmail,
        String bookingStatus,
        BigDecimal amountPaid,
        String paymentMethod,
        OffsetDateTime paidAt
) {
}
