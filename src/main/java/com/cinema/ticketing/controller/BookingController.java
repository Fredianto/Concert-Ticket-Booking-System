package com.cinema.ticketing.controller;

import com.cinema.ticketing.dto.booking.BookingCreateRequest;
import com.cinema.ticketing.dto.booking.BookingResponse;
import com.cinema.ticketing.dto.booking.CancelBookingRequest;
import com.cinema.ticketing.dto.booking.PaymentRequest;
import com.cinema.ticketing.dto.common.StatusResponse;
import com.cinema.ticketing.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public BookingResponse create(@Valid @RequestBody BookingCreateRequest request,
                                  @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader) {
        if (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank() && !idempotencyKeyHeader.equals(request.idempotencyKey())) {
            throw new IllegalArgumentException("Idempotency-Key header must match request.idempotencyKey");
        }
        return bookingService.create(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','VIEWER')")
    public BookingResponse getById(@PathVariable UUID id) {
        return bookingService.getById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER','VIEWER')")
    public List<BookingResponse> listByUser(@RequestParam UUID userId) {
        return bookingService.listByUser(userId);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public StatusResponse cancel(@PathVariable UUID id,
                                 @RequestBody(required = false) CancelBookingRequest request) {
        return new StatusResponse(bookingService.cancel(id, request));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public StatusResponse confirm(@PathVariable UUID id) {
        return new StatusResponse(bookingService.confirm(id));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public StatusResponse pay(@PathVariable UUID id,
                              @Valid @RequestBody PaymentRequest request) {
        return new StatusResponse(bookingService.markPaid(id, request));
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize("hasRole('ADMIN')")
    public StatusResponse deliver(@PathVariable UUID id) {
        return new StatusResponse(bookingService.deliver(id));
    }
}
