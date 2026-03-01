package com.cinema.ticketing.dto.booking;

import jakarta.validation.constraints.NotBlank;

public record PaymentRequest(
        @NotBlank String method,
        String providerRef
) {
}
