# Concert Ticket Booking System (Spring Boot)

Backend API for concert ticket booking with PostgreSQL, JWT auth, Swagger, rate limiting, and reporting.

## Submission Docs
- [Architecture Overview](docs/ARCHITECTURE.md)
- [API Test Evidence](docs/API_TEST_EVIDENCE.md)
- [Requirements Checklist](docs/REQUIREMENTS_CHECKLIST.md)

## Tech Stack
- Java 21
- Spring Boot 3
- PostgreSQL
- JDBC + HikariCP
- JWT (`jjwt`)
- Swagger/OpenAPI (`springdoc`)

## Prerequisites
- PostgreSQL running at `localhost:5432`
- Database objects (`ticketing` schema, tables, functions/procedures) already created in DBeaver
- Java 21 installed
- Maven installed (or use your IDE Maven integration)

## Configuration
Main config is in [src/main/resources/application.yml](src/main/resources/application.yml).

Default DB:
- URL: `jdbc:postgresql://localhost:5432/postgres`
- Username: `cinema`
- Password: `admin`

You can override DB config without code changes:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

If you get `Failed to obtain JDBC Connection` / password auth error, verify PostgreSQL user credentials and restart the app.

Demo auth users:
- `admin / admin123` (role `ADMIN`)
- `user / user123` (role `USER`)

## Run
```bash
mvn spring-boot:run
```

## Swagger
- UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Postman (Recommended for demo)
- Collection: [docs/postman/ticketing.postman_collection.json](docs/postman/ticketing.postman_collection.json)
- Environment: [docs/postman/ticketing.postman_environment.json](docs/postman/ticketing.postman_environment.json)

Run order in Postman:
1. `Get Admin Token` (auto set `token` and `userId`)
2. `Seed Concert + Categories` (auto set `concertId`)
3. `Create Booking` (auto set `bookingId`)
4. `Confirm Booking`
5. `Pay Booking`
6. `Deliver Booking`
7. Reporting endpoints

## Auth Flow (JWT)
1. POST `/api/v1/auth/token`
2. Copy `accessToken`
3. Use header: `Authorization: Bearer <token>`

Sample login payload:
```json
{
  "username": "admin",
  "password": "admin123"
}
```

Login response includes `userId` (auto synced to `ticketing.app_user`) so booking flow can run directly.

## Main Endpoints
### Concert
- `GET /api/v1/concerts`
- `GET /api/v1/concerts/{id}`
- `POST /api/v1/concerts` (ADMIN)
- `PUT /api/v1/concerts/{id}` (ADMIN)
- `POST /api/v1/concerts/{id}/categories` (ADMIN)
- `POST /api/v1/concerts/seed` (ADMIN, concert + categories)
- `GET /api/v1/concerts/{id}/pricing`
- `GET /api/v1/concerts/{id}/availability`

### Booking
- `POST /api/v1/bookings`
- `GET /api/v1/bookings/{id}`
- `GET /api/v1/bookings?userId=<uuid>`
- `POST /api/v1/bookings/{id}/cancel`
- `POST /api/v1/bookings/{id}/confirm`
- `POST /api/v1/bookings/{id}/pay`
- `POST /api/v1/bookings/{id}/deliver` (ADMIN)

### Reporting
- `GET /api/v1/concerts/{id}/settlement`
- `GET /api/v1/transactions`
- `GET /api/v1/analytics/dashboard`

### Health
- `GET /actuator/health`

## Notes
- Booking creation uses DB function `ticketing.sp_create_booking` for atomic inventory hold + idempotency handling.
- Booking state transitions use DB functions: `ticketing.sp_confirm_booking`, `ticketing.sp_mark_booking_paid`, `ticketing.sp_deliver_booking`.
- Cancel/refund uses `ticketing.sp_cancel_or_refund_booking`.
- Request rate limit defaults to 100 requests/minute per IP.
- Correlation ID header supported: `X-Correlation-Id`.

Payment payload example (`POST /api/v1/bookings/{id}/pay`):
```json
{
  "method": "CREDIT_CARD",
  "providerRef": "trx-001"
}
```

Seed payload example (`POST /api/v1/concerts/seed`):
```json
{
  "concert": {
    "name": "Coldplay Live",
    "artist": "Coldplay",
    "venue": "Jakarta Stadium",
    "startsAt": "2026-12-20T20:00:00+07:00",
    "timezone": "Asia/Jakarta",
    "status": "UPCOMING",
    "basePrice": 500000,
    "capacity": 10000
  },
  "categories": [
    { "categoryCode": "VIP", "categoryName": "VIP", "basePriceOverride": 1500000, "totalStock": 500 },
    { "categoryCode": "STANDARD", "categoryName": "Standard", "basePriceOverride": 700000, "totalStock": 3000 },
    { "categoryCode": "GA", "categoryName": "General Admission", "basePriceOverride": 400000, "totalStock": 6500 }
  ]
}
```
