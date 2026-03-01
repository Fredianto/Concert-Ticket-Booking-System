package com.cinema.ticketing.dto.concert;

import java.math.BigDecimal;

public record PricingResponse(
        String categoryCode,
        BigDecimal basePrice,
        BigDecimal demandMultiplier,
        BigDecimal currentPrice,
        Integer totalStock,
        Integer soldStock,
        Integer availableStock
) {
}
