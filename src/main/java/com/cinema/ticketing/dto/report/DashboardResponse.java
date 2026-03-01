package com.cinema.ticketing.dto.report;

import java.math.BigDecimal;

public record DashboardResponse(
        long totalBookings,
        BigDecimal totalRevenue,
        long failedTransactions
) {
}
