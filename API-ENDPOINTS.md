# fare-payment-service — API Endpoints

Base URL: `http://localhost:8084`
Auth: JWT (`Authorization: Bearer <token>`) — this service **validates only**; it never issues tokens. Get a token from `account-service`'s `/api/auth/login` or `/api/auth/register`.

Fare formula: `totalFare = baseFare + (distanceKm × perKmRate)`, rounded to 2 dp.
Defaults (overridable via env vars `FARE_BASE`, `FARE_PER_KM`, `FARE_CURRENCY`): `baseFare = 2.50`, `perKmRate = 1.20`, `currency = USD`.

| # | Method | Path | Auth | Purpose |
|---|--------|------|------|---------|
| 1 | POST | `/api/fares` | Bearer | Calculate and record a fare for a ride |
| 2 | GET | `/api/fares` | Bearer | List fares (optionally filter by `riderId`) |
| 3 | GET | `/api/fares/{id}` | Bearer | Get one fare |
| 4 | GET | `/api/fares/by-ride/{rideId}` | Bearer | Get the fare for a given ride |
| 5 | PATCH | `/api/fares/{id}/void` | Bearer | Void a fare (can no longer be paid) |
| 6 | POST | `/api/payments` | Bearer | Process a simulated payment against a fare |
| 7 | GET | `/api/payments` | Bearer | List payments (filter by `riderId` or `fareId`) |
| 8 | GET | `/api/payments/{id}` | Bearer | Get one payment (the receipt) |
| 9 | POST | `/api/payments/{id}/refund` | Bearer | Refund a completed payment |
| 10 | GET | `/actuator/health` | Public | Liveness check |

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
  "baseFare": 2.5, "distanceKm": 32.5, "distanceFare": 39.0, "totalFare": 41.5,
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
  "amount": 41.5, "transactionId": "txn_3f6b2b9e-7e2a-4b0e-9f2a-2b6a1d9c5e10",
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

### Notes
- Fare/payment status models: `Fare` → `PENDING → INVOICED → PAID` (or `VOID`); `Payment` → `PENDING → COMPLETED/FAILED` (or `REFUNDED` after completion).
- The simulated payment gateway currently always succeeds (`PaymentService.simulateGateway()`) — the `422` example above documents the code path, not a reproducible failure with the current implementation.
- See the [Postman collection](postman/fare-payment-service.postman_collection.json) for a runnable end-to-end sequence.
