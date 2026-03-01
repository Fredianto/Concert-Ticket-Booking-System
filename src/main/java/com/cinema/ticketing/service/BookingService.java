package com.cinema.ticketing.service;

import com.cinema.ticketing.dto.booking.BookingCreateRequest;
import com.cinema.ticketing.dto.booking.BookingResponse;
import com.cinema.ticketing.dto.booking.CancelBookingRequest;
import com.cinema.ticketing.dto.booking.PaymentRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final JdbcTemplate jdbcTemplate;

    public BookingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public BookingResponse create(BookingCreateRequest request) {
        String existingSql = """
                SELECT id, booking_code, status::text, hold_expires_at, total_amount
                FROM ticketing.booking
                WHERE user_id = ? AND idempotency_key = ?
                """;

        BookingResponse existing = jdbcTemplate.query(existingSql, (rs, rn) -> new BookingResponse(
                UUID.fromString(rs.getString("id")),
                rs.getString("booking_code"),
                rs.getString("status"),
                rs.getObject("hold_expires_at", OffsetDateTime.class),
                rs.getBigDecimal("total_amount")
        ), request.userId(), request.idempotencyKey()).stream().findFirst().orElse(null);

        if (existing != null) {
            return existing;
        }

        String concertStatus = jdbcTemplate.queryForObject(
                "SELECT status::text FROM ticketing.concert WHERE id = ?",
                String.class,
                request.concertId());

        if (concertStatus == null) {
            throw new IllegalArgumentException("Concert not found");
        }
        if ("COMPLETED".equals(concertStatus) || "CANCELLED".equals(concertStatus)) {
            throw new IllegalArgumentException("Concert is not bookable");
        }

        String categorySql = """
                SELECT cc.id,
                       cc.total_stock,
                       cc.sold_stock,
                       COALESCE(cc.base_price_override, c.base_price) AS base_price
                FROM ticketing.concert_category cc
                JOIN ticketing.concert c ON c.id = cc.concert_id
                WHERE cc.concert_id = ?
                  AND cc.category_code = ?::ticketing.ticket_category_code
                """;

        CategoryRow category = jdbcTemplate.query(categorySql, (rs, rn) -> new CategoryRow(
                UUID.fromString(rs.getString("id")),
                rs.getInt("total_stock"),
                rs.getInt("sold_stock"),
                rs.getBigDecimal("base_price")
        ), request.concertId(), request.categoryCode().toUpperCase()).stream().findFirst().orElse(null);

        if (category == null) {
            throw new IllegalArgumentException("Concert category not found");
        }

        jdbcTemplate.update("""
                UPDATE ticketing.inventory_hold
                SET status = 'EXPIRED'::ticketing.hold_status, released_at = now()
                WHERE status = 'ACTIVE'::ticketing.hold_status AND expires_at <= now()
                """);

        Integer activeHold = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(quantity), 0)
                FROM ticketing.inventory_hold
                WHERE concert_category_id = ?
                  AND status = 'ACTIVE'::ticketing.hold_status
                  AND expires_at > now()
                """, Integer.class, category.categoryId());

        int available = category.totalStock() - category.soldStock() - (activeHold == null ? 0 : activeHold);
        if (available < request.quantity()) {
            throw new IllegalArgumentException("Insufficient stock");
        }

        BigDecimal availabilityRatio = category.totalStock() == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(available).divide(BigDecimal.valueOf(category.totalStock()), 6, java.math.RoundingMode.HALF_UP);

        BigDecimal demandMultiplier = jdbcTemplate.queryForObject(
                "SELECT ticketing.fn_calc_demand_multiplier(?)",
                BigDecimal.class,
                availabilityRatio);

        BigDecimal unitPrice = category.basePrice().multiply(BigDecimal.ONE.add(demandMultiplier))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.quantity()))
                .setScale(2, java.math.RoundingMode.HALF_UP);

        UUID bookingId = UUID.randomUUID();
        String bookingCode = generateBookingCode(bookingId);
        int holdMinutes = request.holdMinutes() == null ? 5 : request.holdMinutes();
        OffsetDateTime holdExpiresAt = OffsetDateTime.now().plusMinutes(holdMinutes);

        jdbcTemplate.update("""
                INSERT INTO ticketing.booking(
                    id, booking_code, user_id, concert_id, status, hold_expires_at,
                    subtotal_amount, total_amount, currency, idempotency_key, correlation_id, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, 'PENDING'::ticketing.booking_status, ?, ?, ?, 'IDR', ?, ?, now(), now())
                """,
                bookingId,
                bookingCode,
                request.userId(),
                request.concertId(),
                holdExpiresAt,
                totalAmount,
                totalAmount,
                request.idempotencyKey(),
                request.correlationId());

        jdbcTemplate.update("""
                INSERT INTO ticketing.booking_item(id, booking_id, concert_category_id, quantity, unit_price, line_total, created_at)
                VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, now())
                """,
                bookingId,
                category.categoryId(),
                request.quantity(),
                unitPrice,
                totalAmount);

        jdbcTemplate.update("""
                INSERT INTO ticketing.inventory_hold(id, booking_id, concert_category_id, quantity, status, expires_at, created_at)
                VALUES (gen_random_uuid(), ?, ?, ?, 'ACTIVE'::ticketing.hold_status, ?, now())
                """,
                bookingId,
                category.categoryId(),
                request.quantity(),
                holdExpiresAt);

        return new BookingResponse(bookingId, bookingCode, "PENDING", holdExpiresAt, totalAmount);
    }

    public BookingResponse getById(UUID bookingId) {
        String sql = """
                SELECT id, booking_code, status::text, hold_expires_at, total_amount
                FROM ticketing.booking
                WHERE id = ?
                """;

        return jdbcTemplate.query(sql, (rs, rn) -> new BookingResponse(
                UUID.fromString(rs.getString("id")),
                rs.getString("booking_code"),
                rs.getString("status"),
                rs.getObject("hold_expires_at", OffsetDateTime.class),
                rs.getBigDecimal("total_amount")
        ), bookingId).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Booking not found"));
    }

    public List<BookingResponse> listByUser(UUID userId) {
        String sql = """
                SELECT id, booking_code, status::text, hold_expires_at, total_amount
                FROM ticketing.booking
                WHERE user_id = ?
                ORDER BY created_at DESC
                """;

        return jdbcTemplate.query(sql, (rs, rn) -> new BookingResponse(
                UUID.fromString(rs.getString("id")),
                rs.getString("booking_code"),
                rs.getString("status"),
                rs.getObject("hold_expires_at", OffsetDateTime.class),
                rs.getBigDecimal("total_amount")
        ), userId);
    }

    @Transactional
    public String cancel(UUID bookingId, CancelBookingRequest request) {
        String status = getStatus(bookingId);
        BigDecimal refundAmount = request != null && request.refundAmount() != null ? request.refundAmount() : BigDecimal.ZERO;
        String reason = request != null ? request.reason() : null;

        if ("PENDING".equals(status) || "CONFIRMED".equals(status)) {
            jdbcTemplate.update("""
                    UPDATE ticketing.inventory_hold
                    SET status = 'RELEASED'::ticketing.hold_status, released_at = now()
                    WHERE booking_id = ? AND status = 'ACTIVE'::ticketing.hold_status
                    """, bookingId);

            jdbcTemplate.update("""
                    UPDATE ticketing.booking
                    SET status = 'CANCELLED'::ticketing.booking_status,
                        cancel_reason = ?,
                        cancelled_at = now(),
                        updated_at = now()
                    WHERE id = ?
                    """, reason, bookingId);
            return "CANCELLED";
        }

        if ("PAID".equals(status) || "DELIVERED".equals(status)) {
            jdbcTemplate.update("""
                    UPDATE ticketing.concert_category cc
                    SET sold_stock = GREATEST(0, sold_stock - bi.quantity)
                    FROM ticketing.booking_item bi
                    WHERE bi.booking_id = ? AND bi.concert_category_id = cc.id
                    """, bookingId);

            BigDecimal amount = refundAmount.compareTo(BigDecimal.ZERO) > 0
                    ? refundAmount
                    : jdbcTemplate.queryForObject("SELECT total_amount FROM ticketing.booking WHERE id = ?", BigDecimal.class, bookingId);

            jdbcTemplate.update("""
                    INSERT INTO ticketing.payment_transaction(id, booking_id, method, amount, status, provider_ref, created_at, paid_at)
                    VALUES (gen_random_uuid(), ?, 'BANK_TRANSFER'::ticketing.payment_method, ?,
                            CASE WHEN ? < (SELECT total_amount FROM ticketing.booking WHERE id = ?)
                                 THEN 'PARTIAL_REFUND'::ticketing.payment_status
                                 ELSE 'REFUNDED'::ticketing.payment_status END,
                            NULL, now(), now())
                    """, bookingId, amount, amount, bookingId);

            jdbcTemplate.update("""
                    UPDATE ticketing.booking
                    SET status = 'REFUNDED'::ticketing.booking_status,
                        cancel_reason = ?,
                        cancelled_at = now(),
                        updated_at = now()
                    WHERE id = ?
                    """, reason, bookingId);
            return "REFUNDED";
        }

        return status;
    }

    @Transactional
    public String confirm(UUID bookingId) {
        String status = getStatus(bookingId);
        if ("CONFIRMED".equals(status)) {
            return status;
        }
        if (!"PENDING".equals(status)) {
            throw new IllegalArgumentException("Invalid status transition to CONFIRMED");
        }

        Integer updated = jdbcTemplate.update("""
                UPDATE ticketing.booking
                SET status = 'CONFIRMED'::ticketing.booking_status, updated_at = now()
                WHERE id = ? AND hold_expires_at > now()
                """, bookingId);

        if (updated == 0) {
            throw new IllegalArgumentException("Hold expired or booking not found");
        }
        return "CONFIRMED";
    }

    @Transactional
    public String markPaid(UUID bookingId, PaymentRequest request) {
        String status = getStatus(bookingId);
        if ("PAID".equals(status) || "DELIVERED".equals(status)) {
            return status;
        }
        if (!"PENDING".equals(status) && !"CONFIRMED".equals(status)) {
            throw new IllegalArgumentException("Invalid status transition to PAID");
        }

        Integer holdQty = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(quantity), 0)
                FROM ticketing.inventory_hold
                WHERE booking_id = ?
                  AND status = 'ACTIVE'::ticketing.hold_status
                  AND expires_at > now()
                """, Integer.class, bookingId);

        if (holdQty == null || holdQty <= 0) {
            throw new IllegalArgumentException("No active hold for this booking");
        }

        jdbcTemplate.update("""
                UPDATE ticketing.concert_category cc
                SET sold_stock = sold_stock + bi.quantity
                FROM ticketing.booking_item bi
                WHERE bi.booking_id = ? AND bi.concert_category_id = cc.id
                """, bookingId);

        jdbcTemplate.update("""
                UPDATE ticketing.inventory_hold
                SET status = 'CONSUMED'::ticketing.hold_status, released_at = now()
                WHERE booking_id = ? AND status = 'ACTIVE'::ticketing.hold_status
                """, bookingId);

        BigDecimal totalAmount = jdbcTemplate.queryForObject(
                "SELECT total_amount FROM ticketing.booking WHERE id = ?",
                BigDecimal.class,
                bookingId);

        jdbcTemplate.update("""
                INSERT INTO ticketing.payment_transaction(id, booking_id, method, amount, status, provider_ref, created_at, paid_at)
                VALUES (gen_random_uuid(), ?, ?::ticketing.payment_method, ?, 'SUCCESS'::ticketing.payment_status, ?, now(), now())
                """,
                bookingId,
                request.method().toUpperCase(),
                totalAmount,
                request.providerRef());

        jdbcTemplate.update("""
                UPDATE ticketing.booking
                SET status = 'PAID'::ticketing.booking_status, updated_at = now()
                WHERE id = ?
                """, bookingId);

        return "PAID";
    }

    @Transactional
    public String deliver(UUID bookingId) {
        String status = getStatus(bookingId);
        if ("DELIVERED".equals(status)) {
            return status;
        }
        if (!"PAID".equals(status)) {
            throw new IllegalArgumentException("Invalid status transition to DELIVERED");
        }

        jdbcTemplate.update("""
                UPDATE ticketing.booking
                SET status = 'DELIVERED'::ticketing.booking_status, updated_at = now()
                WHERE id = ?
                """, bookingId);

        return "DELIVERED";
    }

    private String getStatus(UUID bookingId) {
        String status = jdbcTemplate.queryForObject(
                "SELECT status::text FROM ticketing.booking WHERE id = ?",
                String.class,
                bookingId);

        if (status == null) {
            throw new IllegalArgumentException("Booking not found");
        }
        return status;
    }

    private String generateBookingCode(UUID bookingId) {
        String timestamp = OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = bookingId.toString().replace("-", "").substring(0, 6).toUpperCase();
        return "BK-" + timestamp + "-" + suffix;
    }

    private record CategoryRow(UUID categoryId, Integer totalStock, Integer soldStock, BigDecimal basePrice) {
    }
}
