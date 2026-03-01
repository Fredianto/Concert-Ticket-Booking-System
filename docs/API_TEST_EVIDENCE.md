# API Test Evidence

Date: 2026-03-01

## Environment
- Base URL: `http://localhost:8080`
- Swagger: `/swagger-ui.html`
- Database schema: `ticketing`

## Validated Flows

### 1) Health and Auth
- `GET /actuator/health` -> **200 UP**
- `POST /api/v1/auth/token` -> **200** with JWT + `userId`

### 2) Concert + Category Setup
- `POST /api/v1/concerts/seed` -> **200** with generated `concertId`
- `GET /api/v1/concerts/{id}/pricing` -> returns category pricing
- `GET /api/v1/concerts/{id}/availability` -> returns availability rows

### 3) Booking Lifecycle
- `POST /api/v1/bookings` -> status **PENDING**
- `POST /api/v1/bookings/{id}/confirm` -> status **CONFIRMED**
- `POST /api/v1/bookings/{id}/pay` -> status **PAID**
- `POST /api/v1/bookings/{id}/deliver` -> status **DELIVERED**
- `GET /api/v1/bookings/{id}` -> final status **DELIVERED**

### 4) Reporting Validation
- `GET /api/v1/transactions` -> transaction rows returned
- `GET /api/v1/analytics/dashboard` -> total bookings/revenue metrics returned
- `GET /api/v1/concerts/{id}/settlement` -> settlement rows returned

## Sample Terminal Result Snapshot
- `BOOKING_STATUS=PENDING`
- `CONFIRM_STATUS=CONFIRMED`
- `PAY_STATUS=PAID`
- `DELIVER_STATUS=DELIVERED`
- `DETAIL_STATUS=DELIVERED`
- `TRANSACTION_COUNT=2`
- `DASHBOARD totalBookings=2 totalRevenue=3300000.00 failedTransactions=0`
- `SETTLEMENT_FIRST status=DELIVERED amountPaid=1650000.00`

## Notes
- During implementation, SQL compatibility issues from mixed function/procedure DB objects were resolved.
- Final implementation uses robust transactional SQL logic in service layer for booking lifecycle, compatible with current DB state.
