# fare-payment-service

Calculates fares for completed rides and processes payments against them.

- **Port:** `8084`
- **Database:** `ridelink_payments` (MongoDB)
- **Auth:** validates JWTs issued by `account-service`.
- **IDs:** `Fare.id`, `Payment.id`, and cross-service references (`rideId`, `riderId`, `fareId`) are
  MongoDB ObjectId strings (not numeric).

## Run locally

```bash
mvn spring-boot:run
```

Requires a MongoDB instance reachable at `DB_HOST:DB_PORT` (defaults to `localhost:27017`).
Tests use an embedded in-memory MongoDB automatically — no external database needed to run `mvn test`.

## Environment variables

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | see `application.yml` | MongoDB connection |
| `DB_USERNAME` / `DB_PASSWORD` / `DB_AUTH_DATABASE` | (blank) / (blank) / `admin` | MongoDB credentials, if the instance requires auth |
| `JWT_SECRET` | (dev default) | Must match account-service |
| `FARE_BASE` | `2.50` | Base fare |
| `FARE_PER_KM` | `1.20` | Per-kilometer rate |
| `FARE_CURRENCY` | `USD` | Currency code |

## API

All endpoints require `Authorization: Bearer <token>`.

### Fares

| Method | Path | Description |
|---|---|---|
| POST | `/api/fares` | Calculate and invoice a fare for a completed ride |
| GET | `/api/fares` | List fares (optional `?riderId=`) |
| GET | `/api/fares/{id}` | Get a fare |
| GET | `/api/fares/by-ride/{rideId}` | Get the fare for a given ride |
| PATCH | `/api/fares/{id}/void` | Void a fare (e.g. disputed ride) |

**Calculate fare**
```json
POST /api/fares
{
  "rideId": "77a0b1c2d3e4f5a6b7c8d9e0",
  "riderId": "665f0a1b2c3d4e5f6a7b8c9d",
  "distanceKm": 4.2
}
```
```json
{
  "id": "99c2d3e4f5a6b7c8d9e0f1a2",
  "rideId": "77a0b1c2d3e4f5a6b7c8d9e0",
  "riderId": "665f0a1b2c3d4e5f6a7b8c9d",
  "baseFare": 2.5,
  "distanceKm": 4.2,
  "distanceFare": 5.04,
  "totalFare": 7.54,
  "currency": "USD",
  "status": "INVOICED"
}
```
One fare per `rideId` is enforced — calling this twice for the same ride returns `400`.

### Payments

| Method | Path | Description |
|---|---|---|
| POST | `/api/payments` | Process a payment against a fare |
| GET | `/api/payments` | List payments (optional `?riderId=` or `?fareId=`) |
| GET | `/api/payments/{id}` | Get a payment |
| POST | `/api/payments/{id}/refund` | Refund a completed payment |

**Process payment**
```json
POST /api/payments
{
  "fareId": "99c2d3e4f5a6b7c8d9e0f1a2",
  "riderId": "665f0a1b2c3d4e5f6a7b8c9d",
  "method": "CARD"
}
```
`method` is one of `CARD`, `CASH`, `WALLET`. On success the fare is marked `PAID` and a `transactionId` is returned.

## Notes / production TODOs

- `PaymentService.simulateGateway()` is a stub that always succeeds — swap in a real gateway SDK (Stripe, Braintree, etc.) and handle async webhook confirmation instead of a synchronous result.
- No idempotency key is enforced on `POST /api/payments` yet; add one before wiring up a real client that might retry.
