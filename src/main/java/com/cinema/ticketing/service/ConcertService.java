package com.cinema.ticketing.service;

import com.cinema.ticketing.dto.concert.AvailabilityResponse;
import com.cinema.ticketing.dto.concert.ConcertCategoryUpsertRequest;
import com.cinema.ticketing.dto.concert.ConcertResponse;
import com.cinema.ticketing.dto.concert.ConcertUpsertRequest;
import com.cinema.ticketing.dto.concert.PricingResponse;
import com.cinema.ticketing.dto.concert.SeedConcertRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ConcertService {

    private final JdbcTemplate jdbcTemplate;

    public ConcertService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ConcertResponse> list(String name, String artist, String venue) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, name, artist, venue, starts_at, timezone, status::text, base_price, capacity
                FROM ticketing.concert
                WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();

        if (name != null && !name.isBlank()) {
            sql.append(" AND LOWER(name) LIKE LOWER(?)");
            args.add("%" + name + "%");
        }
        if (artist != null && !artist.isBlank()) {
            sql.append(" AND LOWER(artist) LIKE LOWER(?)");
            args.add("%" + artist + "%");
        }
        if (venue != null && !venue.isBlank()) {
            sql.append(" AND LOWER(venue) LIKE LOWER(?)");
            args.add("%" + venue + "%");
        }
        sql.append(" ORDER BY starts_at");

        return jdbcTemplate.query(sql.toString(), (rs, rn) -> new ConcertResponse(
                UUID.fromString(rs.getString("id")),
                rs.getString("name"),
                rs.getString("artist"),
                rs.getString("venue"),
                rs.getObject("starts_at", java.time.OffsetDateTime.class),
                rs.getString("timezone"),
                rs.getString("status"),
                rs.getBigDecimal("base_price"),
                rs.getInt("capacity")
        ), args.toArray());
    }

    public ConcertResponse getById(UUID id) {
        String sql = """
                SELECT id, name, artist, venue, starts_at, timezone, status::text, base_price, capacity
                FROM ticketing.concert
                WHERE id = ?
                """;
        return jdbcTemplate.query(sql, (rs, rn) -> new ConcertResponse(
                UUID.fromString(rs.getString("id")),
                rs.getString("name"),
                rs.getString("artist"),
                rs.getString("venue"),
                rs.getObject("starts_at", java.time.OffsetDateTime.class),
                rs.getString("timezone"),
                rs.getString("status"),
                rs.getBigDecimal("base_price"),
                rs.getInt("capacity")
        ), id).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Concert not found"));
    }

    public UUID create(ConcertUpsertRequest request) {
        UUID id = UUID.randomUUID();
        String sql = """
                INSERT INTO ticketing.concert(id, name, artist, venue, starts_at, timezone, status, base_price, capacity)
                VALUES (?, ?, ?, ?, ?, ?, ?::ticketing.concert_status, ?, ?)
                """;
        jdbcTemplate.update(sql,
                id,
                request.name(),
                request.artist(),
                request.venue(),
                request.startsAt(),
                request.timezone(),
                request.status().toUpperCase(),
                request.basePrice(),
                request.capacity());
        return id;
    }

    public void update(UUID id, ConcertUpsertRequest request) {
        String sql = """
                UPDATE ticketing.concert
                SET name = ?, artist = ?, venue = ?, starts_at = ?, timezone = ?,
                    status = ?::ticketing.concert_status, base_price = ?, capacity = ?, updated_at = now()
                WHERE id = ?
                """;

        int updated = jdbcTemplate.update(sql,
                request.name(),
                request.artist(),
                request.venue(),
                request.startsAt(),
                request.timezone(),
                request.status().toUpperCase(),
                request.basePrice(),
                request.capacity(),
                id);

        if (updated == 0) {
            throw new IllegalArgumentException("Concert not found");
        }
    }

    @Transactional
    public void upsertCategories(UUID concertId, List<ConcertCategoryUpsertRequest> requests) {
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ticketing.concert WHERE id = ?",
                Integer.class,
                concertId);

        if (exists == null || exists == 0) {
            throw new IllegalArgumentException("Concert not found");
        }

        String sql = """
                INSERT INTO ticketing.concert_category(
                    id, concert_id, category_code, category_name, base_price_override, total_stock, sold_stock
                )
                VALUES (gen_random_uuid(), ?, ?::ticketing.ticket_category_code, ?, ?, ?, 0)
                ON CONFLICT (concert_id, category_code)
                DO UPDATE SET
                    category_name = EXCLUDED.category_name,
                    base_price_override = EXCLUDED.base_price_override,
                    total_stock = EXCLUDED.total_stock
                """;

        for (ConcertCategoryUpsertRequest request : requests) {
            jdbcTemplate.update(sql,
                    concertId,
                    request.categoryCode().toUpperCase(),
                    request.categoryName(),
                    request.basePriceOverride(),
                    request.totalStock());
        }
    }

    @Transactional
    public UUID seedConcert(SeedConcertRequest request) {
        UUID concertId = create(request.concert());
        upsertCategories(concertId, request.categories());
        return concertId;
    }

    public List<PricingResponse> pricing(UUID concertId) {
        String sql = """
                SELECT
                    cc.category_code::text AS category_code,
                    COALESCE(cc.base_price_override, c.base_price) AS base_price,
                    ticketing.fn_calc_demand_multiplier(
                        (ticketing.fn_available_stock(cc.id)::numeric / NULLIF(cc.total_stock, 0))
                    ) AS demand_multiplier,
                    ROUND(
                        COALESCE(cc.base_price_override, c.base_price)
                        * (1 + ticketing.fn_calc_demand_multiplier((ticketing.fn_available_stock(cc.id)::numeric / NULLIF(cc.total_stock, 0)))),
                    2) AS current_price,
                    cc.total_stock,
                    cc.sold_stock,
                    ticketing.fn_available_stock(cc.id) AS available_stock
                FROM ticketing.concert_category cc
                JOIN ticketing.concert c ON c.id = cc.concert_id
                WHERE cc.concert_id = ?
                ORDER BY cc.category_code
                """;

        return jdbcTemplate.query(sql, (rs, rn) -> new PricingResponse(
                rs.getString("category_code"),
                rs.getBigDecimal("base_price"),
                rs.getBigDecimal("demand_multiplier"),
                rs.getBigDecimal("current_price"),
                rs.getInt("total_stock"),
                rs.getInt("sold_stock"),
                rs.getInt("available_stock")
        ), concertId);
    }

    public List<AvailabilityResponse> availability(UUID concertId) {
        String sql = """
                SELECT
                    cc.category_code::text AS category_code,
                    cc.total_stock,
                    cc.sold_stock,
                    ticketing.fn_available_stock(cc.id) AS available_stock
                FROM ticketing.concert_category cc
                WHERE cc.concert_id = ?
                ORDER BY cc.category_code
                """;

        return jdbcTemplate.query(sql, (rs, rn) -> new AvailabilityResponse(
                rs.getString("category_code"),
                rs.getInt("total_stock"),
                rs.getInt("sold_stock"),
                rs.getInt("available_stock")
        ), concertId);
    }
}
