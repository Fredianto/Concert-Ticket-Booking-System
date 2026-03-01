package com.cinema.ticketing.service;

import com.cinema.ticketing.dto.report.DashboardResponse;
import com.cinema.ticketing.dto.report.SettlementRow;
import com.cinema.ticketing.dto.report.TransactionRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ReportingService {

    private final JdbcTemplate jdbcTemplate;

    public ReportingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SettlementRow> settlement(UUID concertId) {
        String sql = """
                SELECT booking_id, booking_code, user_email, booking_status::text, amount_paid, payment_method::text, paid_at
                FROM ticketing.fn_settlement_report(?::uuid)
                """;

        return jdbcTemplate.query(sql, (rs, rn) -> new SettlementRow(
                UUID.fromString(rs.getString("booking_id")),
                rs.getString("booking_code"),
                rs.getString("user_email"),
                rs.getString("booking_status"),
                rs.getBigDecimal("amount_paid"),
                rs.getString("payment_method"),
                rs.getObject("paid_at", java.time.OffsetDateTime.class)
        ), concertId);
    }

    public List<TransactionRow> transactions() {
        String sql = """
                SELECT id, booking_id, method::text, amount, status::text, provider_ref, created_at, paid_at
                FROM ticketing.payment_transaction
                ORDER BY created_at DESC
                """;

        return jdbcTemplate.query(sql, (rs, rn) -> new TransactionRow(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("booking_id")),
                rs.getString("method"),
                rs.getBigDecimal("amount"),
                rs.getString("status"),
                rs.getString("provider_ref"),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("paid_at", java.time.OffsetDateTime.class)
        ));
    }

    public DashboardResponse dashboard() {
        Long totalBookings = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ticketing.booking", Long.class);
        BigDecimal totalRevenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM ticketing.payment_transaction WHERE status = 'SUCCESS'", BigDecimal.class);
        Long failedTx = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ticketing.payment_transaction WHERE status IN ('FAILED')", Long.class);

        return new DashboardResponse(
                totalBookings == null ? 0 : totalBookings,
                totalRevenue == null ? BigDecimal.ZERO : totalRevenue,
                failedTx == null ? 0 : failedTx
        );
    }
}
