# fare-payment-service — API Endpoints

Base URL: `http://localhost:8084`
Auth: JWT (`Authorization: Bearer <token>`) — this service **validates only**; it never issues tokens. Get a token from `account-service`'s `/api/auth/login` or `/api/auth/register`.

Fare rule (one rule for estimates and final fares, implemented in `FareCalculator`):

```
durationMinutes = supplied value, or round(distanceKm / averageSpeedKmh x 60) if omitted
distanceFare    = distanceKm      x perKmRate
timeFare        = durationMinutes x perMinuteRate
subtotal        = baseFare + distanceFare + timeFare
totalFare       = max(minimumFare, subtotal)            -- all amounts rounded to 2 dp
```

Defaults (env var overrides): `baseFare 2.50` (`FARE_BASE`), `perKmRate 1.20` (`FARE_PER_KM`), `perMinuteRate 0.20` (`FARE_PER_MINUTE`),
`minimumFare 5.00` (`FARE_MINIMUM`), `averageSpeedKmh 30` (`FARE_AVG_SPEED_KMH`), `currency USD` (`FARE_CURRENCY`).

Worked example: 10 km, 25 min -> 2.50 + 12.00 + 5.00 = **19.50**. Short trip 1 km, 2 min -> subtotal 4.10 -> minimum **5.00**.

| # | Method | Path | Auth | Purpose |
|---|--------|------|------|---------|
| 1 | POST | `/api/fares` | Bearer | Calculate and record a fare for a ride |
| 2 | GET | `/api/fares` | Bearer | List fares (optionally filter by `riderId`) |
| 3 | GET | `/api/fares/{id}` | Bearer | Get one fare |
| 4 | GET | `/api/fares/by-ride/{rideId}` | Bearer | Get the fare for a given ride |
| 5 | PATCH | `/api/fares/{id}/void` | Bearer | Void a fare (can no longer be paid) |
| 6 | POST | `/api/payments` | Bearer | Process a simulated payment against a fare |
| 7 | GET | `/api/payments` | Bearer | List payments (filter by `riderId`, `fareId` or `rideId`) |
| 8 | GET | `/api/payments/{id}` | Bearer | Get one payment |
| 9 | POST | `/api/payments/{id}/refund` | Bearer | Refund a completed payment |
| 10 | POST | `/api/fares/estimate` | Bearer | Fare estimate (not stored) |
| 11 | GET | `/api/payments/{id}/status` | Bearer | Payment status |
| 12 | GET | `/api/payments/{id}/receipt` | Bearer | Receipt for a payment |
| 13 | GET | `/api/receipts` | Bearer | List receipts (`riderId` / `rideId`) |
| 14 | GET | `/api/receipts/{id}` | Bearer | Get one receipt |
| 15 | GET | `/api/receipts/by-number/{receiptNumber}` | Bearer | Receipt by number |
| 16 | GET | `/actuator/health` | Public | Liveness check |

---

## Fares

### 1. `POST /api/fares`

**Request**
```json
{ "rideId": "77a0b1c2d3e4f5a6b7c8d9e0", "riderId": "665f0a1b2c3d4e5f6a7b8c9d", "distanceKm": 32.5 }
```
`rideId`, `riderId`, `distanceKm` (>= 0) required. Only one fare can ever be calculated per `rideId`. New fares start `INVOICED`.

**Response `201 Created`**
```json
{
  "id": "99c2d3e4f5a6b7c8d9e0f1a2", "rideId": "77a0b1c2d3e4f5a6b7c8d9e0", "riderId": "665f0a1b2c3d4e5f6a7b8c9d",
  "baseFare": 2.5, "distanceKm": 32.5, "distanceFare": 39.0, "durationMinutes": 65, "timeFare": 13.0, "subtotal": 54.5, "minimumFareApplied": false, "totalFare": 54.5,
  "currency": "USD", "status": "INVOICED", "createdAt": "2026-09-13T16:40:00Z"
}
```

**Negative — `400 Bad Request`** (fare already exists for this ride)
```json
{ "timestamp": "2026-09-13T16:41:00Z", "status": 400, "error": "Bad Request", "message": "A fare has already been calculated for rideId: 77a0b1c2d3e4f5a6b7c8d9e0" }
```

**Negative — `400 Bad Request`** (invalid input)
```json
{ "timestamp": "2026-09-13T16:41:30Z", "status": 400, "error": "Bad Request", "message": "Validation failed", "details": ["distanceKm: must be greater than or equal to 0"] }
```

```bash
curl -X POST http://localhost:8084/api/fares -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"rideId":"77a0b1c2d3e4f5a6b7c8d9e0","riderId":"665f0a1b2c3d4e5f6a7b8c9d","distanceKm":32.5}'
```

### 2. `GET /api/fares?riderId={id}`

Omit `riderId` to list every fare.

### 3. `GET /api/fares/{id}`

`404` if not found.

### 4. `GET /api/fares/by-ride/{rideId}`

Looks up the fare already calculated for a ride-management-service `rideId`. `404` if none exists yet.

### 5. `PATCH /api/fares/{id}/void`

No body. **Response `204 No Content`.**

---

## Payments

### 6. `POST /api/payments`

**Request**
```json
{ "fareId": "99c2d3e4f5a6b7c8d9e0f1a2", "riderId": "665f0a1b2c3d4e5f6a7b8c9d", "method": "CARD" }
```
`method` is one of `CARD`, `CASH`, `WALLET`. The amount is taken from the fare's `totalFare`, not sent by the client. On success the fare is marked `PAID`.

**Response `201 Created`**
```json
{
  "id": "aab3d4e5f6a7b8c9d0e1f2a3", "fareId": "99c2d3e4f5a6b7c8d9e0f1a2", "riderId": "665f0a1b2c3d4e5f6a7b8c9d", "method": "CARD", "status": "COMPLETED",
  "amount": 54.5, "transactionId": "txn_3f6b2b9e-7e2a-4b0e-9f2a-2b6a1d9c5e10",
  "paidAt": "2026-09-13T16:45:00Z", "createdAt": "2026-09-13T16:45:00Z"
}
```

**Negative — `422 Unprocessable Entity`** (simulated gateway failure)
```json
{ "timestamp": "2026-09-13T16:46:00Z", "status": 422, "error": "Unprocessable Entity", "message": "Payment could not be processed. Please try again." }
```

**Negative — `400 Bad Request`** (fare already paid)
```json
{ "timestamp": "2026-09-13T16:47:00Z", "status": 400, "error": "Bad Request", "message": "This fare has already been paid" }
```

```bash
curl -X POST http://localhost:8084/api/payments -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"fareId":"99c2d3e4f5a6b7c8d9e0f1a2","riderId":"665f0a1b2c3d4e5f6a7b8c9d","method":"CARD"}'
```

### 7. `GET /api/payments?riderId={id}` / `?fareId={id}`

`riderId` takes precedence if both are supplied. Omit both to list every payment.

### 8. `GET /api/payments/{id}`

The receipt referenced in workflow 6. `404` if not found.

### 9. `POST /api/payments/{id}/refund`

No body. Only valid on a `COMPLETED` payment — moves it to `REFUNDED`.

**Negative — `400 Bad Request`**
```json
{ "timestamp": "2026-09-13T16:49:00Z", "status": 400, "error": "Bad Request", "message": "Only completed payments can be refunded" }
```

### 10. `GET /actuator/health`

```json
{ "status": "UP" }
```

---

## Fare estimation, payment status & receipts

### `POST /api/fares/estimate` - quote only, nothing stored
```json
{ "distanceKm": 10, "durationMinutes": 25 }
```
**Response `200 OK`**
```json
{
  "distanceKm": 10.0, "durationMinutes": 25, "durationEstimated": false,
  "baseFare": 2.5, "distanceFare": 12.0, "timeFare": 5.0, "subtotal": 19.5,
  "minimumFare": 5.0, "minimumFareApplied": false, "totalFare": 19.5, "currency": "USD",
  "rule": "totalFare = max(minimumFare, baseFare + distanceKm x perKmRate + durationMinutes x perMinuteRate)"
}
```
`POST /api/fares` (final fare) accepts the same optional `durationMinutes` and returns the same breakdown fields.

### Simulated payments
`POST /api/payments` accepts an optional `"simulateFailure": true` to demo a declined payment: the payment is stored
as `FAILED` with a `failureReason` and the call returns `422`. The fare stays payable.
Payments now also record `rideId`, `refundedAt` and `updatedAt`.

### `GET /api/payments/{id}/status`
```json
{ "paymentId": "...", "status": "COMPLETED", "amount": 19.5, "transactionId": "txn_...", "receiptNumber": "RL-20260925-4F7A1C", "updatedAt": "..." }
```

### Receipts
A receipt is generated automatically for every successful payment. It is a snapshot of the fare breakdown and payment
(receipt number format `RL-yyyyMMdd-XXXXXX`). Refunding the payment marks the receipt `REFUNDED` with `refundedAt`.

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/payments/{id}/receipt` | Receipt for a payment (issued on demand for older payments; `400` if the payment is not completed/refunded) |
| GET | `/api/receipts` | List receipts, filter by `riderId` or `rideId` |
| GET | `/api/receipts/{id}` | One receipt |
| GET | `/api/receipts/by-number/{receiptNumber}` | Look up by receipt number |

```json
{
  "id": "...", "receiptNumber": "RL-20260925-4F7A1C", "paymentId": "...", "fareId": "...", "rideId": "...", "riderId": "...",
  "method": "CARD", "transactionId": "txn_...",
  "baseFare": 2.5, "distanceKm": 10.0, "distanceFare": 12.0, "durationMinutes": 25, "timeFare": 5.0, "minimumFareApplied": false,
  "totalPaid": 19.5, "currency": "USD", "paymentStatus": "COMPLETED", "paidAt": "...", "issuedAt": "..."
}
```

---

### Notes
- Fare/payment status models: `Fare` → `PENDING → INVOICED → PAID` (or `VOID`); `Payment` → `PENDING → COMPLETED/FAILED` (or `REFUNDED` after completion).
- The simulated gateway succeeds unless the request sets `"simulateFailure": true`, which reproduces the `422` path above.
- See the [Postman collection](postman/fare-payment-service.postman_collection.json) for a runnable end-to-end sequence.
