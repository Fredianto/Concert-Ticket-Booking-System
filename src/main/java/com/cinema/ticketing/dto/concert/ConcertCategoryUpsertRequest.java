package com.cinema.ticketing.dto.concert;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ConcertCategoryUpsertRequest(
        @NotBlank String categoryCode,
        @NotBlank String categoryName,
        BigDecimal basePriceOverride,
        @NotNull @Min(0) Integer totalStock
) {
}
