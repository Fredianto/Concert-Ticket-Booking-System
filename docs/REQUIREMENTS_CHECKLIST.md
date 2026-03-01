# Requirements Checklist

## Functional Requirements

### 1. Concert Management
- [x] Create concert (`POST /api/v1/concerts`)
- [x] Update concert (`PUT /api/v1/concerts/{id}`)
- [x] List and retrieve concerts (`GET /api/v1/concerts`, `GET /api/v1/concerts/{id}`)
- [x] Multi-timezone field supported (`timezone`)
- [x] Concert status supported (`UPCOMING`, `ONGOING`, `COMPLETED`, `CANCELLED`)

### 2. Inventory & Pricing
- [x] Multi ticket categories (`VIP`, `STANDARD`, `GA`)
- [x] Dynamic pricing endpoint (`GET /api/v1/concerts/{id}/pricing`)
- [x] Availability endpoint (`GET /api/v1/concerts/{id}/availability`)
- [x] Oversell prevention logic in transactional booking creation

### 3. User Order Management
- [x] Create booking (`POST /api/v1/bookings`)
- [x] Booking state transitions (`confirm`, `pay`, `deliver`, `cancel`)
- [x] Hold period support (`holdMinutes`)
- [x] Idempotency support (`Idempotency-Key` + request key)

### 4. Settlement & Ledger
- [x] Transaction endpoint (`GET /api/v1/transactions`)
- [x] Settlement report endpoint (`GET /api/v1/concerts/{id}/settlement`)
- [x] Refund/cancel flow integrated (`POST /api/v1/bookings/{id}/cancel`)
- [ ] Full immutable ledger write on every transition at app layer (partially delegated to DB design)

### 5. Search & Analytics
- [x] Concert filtering by name/artist/venue
- [x] Dashboard metrics endpoint (`GET /api/v1/analytics/dashboard`)
- [x] User booking history (`GET /api/v1/bookings?userId=...`)

## Non-Functional Requirements

### Performance & Scalability
- [x] HikariCP enabled
- [ ] Proven load test evidence for 1,000 RPS (not included yet)

### Consistency & Reliability
- [x] Transactional booking lifecycle in service layer
- [x] Idempotent booking creation

### Security & Validation
- [x] JWT bearer token validation
- [x] RBAC (`ADMIN`, `USER`, `VIEWER`)
- [x] Input validation using Jakarta Validation
- [x] Rate limiting filter (100 requests/minute/IP)

### Observability
- [x] Structured JSON logs
- [x] Correlation ID support
- [x] Health endpoint via actuator

## Bonus/Optional
- [ ] Redis cache (not implemented)
- [ ] MQ integration (not implemented)
- [ ] Automated integration/load tests (not implemented)
