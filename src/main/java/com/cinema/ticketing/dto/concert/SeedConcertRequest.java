package com.cinema.ticketing.dto.concert;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SeedConcertRequest(
        @Valid @NotNull ConcertUpsertRequest concert,
        @Valid @NotEmpty List<ConcertCategoryUpsertRequest> categories
) {
}
