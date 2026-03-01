# Architecture Overview

## High-Level Design
The backend uses a layered Spring Boot architecture:
- **Controller Layer**: Exposes REST endpoints under `/api/v1/**`
- **Service Layer**: Contains business logic for auth, concert, booking, and reporting
- **Data Access**: Uses `JdbcTemplate` with PostgreSQL (`ticketing` schema)
- **Security Layer**: JWT bearer auth + RBAC + rate limit + correlation ID

## Core Components
- **Authentication**
  - `POST /api/v1/auth/token`
  - In-memory credential config from `application.yml`
  - JWT token contains `sub` and `role`
  - Auto-sync authenticated user to `ticketing.app_user`

- **Concert Management**
  - CRUD-like operations for concert (create/update/list/detail)
  - Category upsert per concert (VIP/STANDARD/GA)
  - Seed endpoint for demo (`/api/v1/concerts/seed`)

- **Inventory & Dynamic Pricing**
  - Real-time availability from `concert_category` + active `inventory_hold`
  - Pricing computed using demand multiplier function in DB:
    - `fn_calc_demand_multiplier`
    - `fn_available_stock`

- **Booking Lifecycle**
  - Create booking with idempotency (`user_id + idempotency_key`)
  - Hold inventory for configurable minutes
  - State transition flow:
    - `PENDING -> CONFIRMED -> PAID -> DELIVERED`
    - plus `CANCELLED/REFUNDED`
  - Atomic operations handled in transactional service methods

- **Reporting/Settlement**
  - Transaction list endpoint
  - Dashboard metrics endpoint
  - Settlement per concert endpoint

## Security & Reliability
- **JWT Validation**: `Authorization: Bearer <token>`
- **RBAC** using Spring Method Security (`@PreAuthorize`)
- **Rate Limiting**: max 100 requests/minute per IP
- **Correlation ID**: propagated via `X-Correlation-Id`
- **Structured Logging**: JSON log format (`logback-spring.xml`)
- **Connection Pooling**: HikariCP

## Deployment Notes
- Default DB connection targets:
  - `jdbc:postgresql://localhost:5432/postgres?currentSchema=ticketing`
  - user: `cinema`
- Config can be overridden with env vars:
  - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`

## Future Improvements
- Replace in-memory users with persistent user table + hashed passwords
- Add dedicated payment integration adapter + circuit breaker
- Add TestContainers integration tests and load tests
- Add Redis cache for pricing/availability hot paths
- Add async events for booking/payment/settlement streams
