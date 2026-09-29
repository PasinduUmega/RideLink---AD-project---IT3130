# ride-management-service — API Endpoints

Base URL: `http://localhost:8083`
Auth: JWT (`Authorization: Bearer <token>`) — this service **validates only**; it never issues tokens. Get a token from `account-service`'s `/api/auth/login` or `/api/auth/register`.

| # | Method | Path | Auth | Purpose |
|---|--------|------|------|---------|
| 1 | POST | `/api/rides` | Bearer | Create a ride request |
| 2 | GET | `/api/rides` | Bearer | List rides (filter by `riderId`, `driverId` or `status`) |
| 3 | GET | `/api/rides/{id}` | Bearer | Get one ride |
| 4 | PATCH | `/api/rides/{id}/accept` | Bearer | Driver accepts the ride |
| 5 | PATCH | `/api/rides/{id}/start` | Bearer | Mark ride in progress |
| 6 | PATCH | `/api/rides/{id}/complete` | Bearer | Mark ride completed |
| 7 | PATCH | `/api/rides/{id}/cancel` | Bearer | Cancel a ride |
| 8 | GET | `/actuator/health` | Public | Liveness check |

**Ride lifecycle:** `REQUESTED → ACCEPTED → IN_PROGRESS → COMPLETED`, or `REQUESTED/ACCEPTED/IN_PROGRESS → CANCELLED`. Any other transition is rejected with `409 Conflict`.

---

## 1. `POST /api/rides`

**Request**
```json
{
  "riderId": "665f0a1b2c3d4e5f6a7b8c9d",
  "pickupLocation": "Colombo Fort Railway Station",
  "dropoffLocation": "Bandaranaike International Airport",
  "pickupLat": 6.9344, "pickupLng": 79.8428,
  "dropoffLat": 7.1808, "dropoffLng": 79.8841,
  "distanceKm": 32.5
}
```
`riderId`, `pickupLocation`, `dropoffLocation` required. Lat/lng and `distanceKm` optional — if `distanceKm` is given, `estimatedFare` is computed immediately (`baseFare + distanceKm × perKmRate`).

**Response `201 Created`**
```json
{
  "id": "77a0b1c2d3e4f5a6b7c8d9e0", "riderId": "665f0a1b2c3d4e5f6a7b8c9d", "driverId": null,
  "pickupLocation": "Colombo Fort Railway Station", "dropoffLocation": "Bandaranaike International Airport",
  "status": "REQUESTED", "distanceKm": 32.5, "estimatedFare": 41.5,
  "requestedAt": "2026-09-13T16:00:00Z"
}
```

**Negative — `400 Bad Request`**
```json
{ "timestamp": "2026-09-13T16:00:00Z", "status": 400, "error": "Bad Request", "message": "Validation failed", "details": ["riderId: must not be blank", "pickupLocation: must not be blank"] }
```

```bash
curl -X POST http://localhost:8083/api/rides -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"riderId":"665f0a1b2c3d4e5f6a7b8c9d","pickupLocation":"Colombo Fort","dropoffLocation":"Airport","distanceKm":32.5}'
```

---

## 2. `GET /api/rides`

Query params (apply one at a time): `riderId`, `driverId`, `status` (`REQUESTED`/`ACCEPTED`/`IN_PROGRESS`/`COMPLETED`/`CANCELLED`). Omit all to list every ride.

```bash
curl "http://localhost:8083/api/rides?status=REQUESTED" -H "Authorization: Bearer $TOKEN"
```

---

## 3. `GET /api/rides/{id}`

**Negative — `404 Not Found`**
```json
{ "timestamp": "2026-09-13T16:00:00Z", "status": 404, "error": "Not Found", "message": "Ride not found with id: 000000000000000000000000" }
```

---

## 4. `PATCH /api/rides/{id}/accept`

**Request**
```json
{ "driverId": "665f1a2b3c4d5e6f7a8b9c0d" }
```
Only valid when the ride is `REQUESTED`. `driverId` typically comes from `GET /api/drivers?status=APPROVED` on driver-vehicle-service (a MongoDB ObjectId string).

**Response `200 OK`**
```json
{ "id": "77a0b1c2d3e4f5a6b7c8d9e0", "riderId": "665f0a1b2c3d4e5f6a7b8c9d", "driverId": "665f1a2b3c4d5e6f7a8b9c0d", "status": "ACCEPTED", "distanceKm": 32.5, "estimatedFare": 41.5, "acceptedAt": "2026-09-13T16:01:00Z" }
```

**Negative — `409 Conflict`** (already accepted / not in REQUESTED state)
```json
{ "timestamp": "2026-09-13T16:01:30Z", "status": 409, "error": "Conflict", "message": "Ride is ACCEPTED, expected REQUESTED for this action" }
```

---

## 5. `PATCH /api/rides/{id}/start`

No body. Only valid when `ACCEPTED`.

**Negative — `409 Conflict`** (start before accept)
```json
{ "timestamp": "2026-09-13T16:05:00Z", "status": 409, "error": "Conflict", "message": "Ride is REQUESTED, expected ACCEPTED for this action" }
```

---

## 6. `PATCH /api/rides/{id}/complete`

No body. Only valid when `IN_PROGRESS`. Recomputes `estimatedFare` if `distanceKm` is set.

**Response `200 OK`**
```json
{ "id": "77a0b1c2d3e4f5a6b7c8d9e0", "status": "COMPLETED", "distanceKm": 32.5, "estimatedFare": 41.5, "completedAt": "2026-09-13T16:35:00Z" }
```

> Next step: call `fare-payment-service`'s `POST /api/fares` with this `rideId` to record the actual fare.

---

## 7. `PATCH /api/rides/{id}/cancel`

**Request** (optional body)
```json
{ "reason": "Rider changed their mind" }
```
Valid from `REQUESTED`, `ACCEPTED`, or `IN_PROGRESS`.

**Negative — `409 Conflict`** (already completed/cancelled)
```json
{ "timestamp": "2026-09-13T16:36:00Z", "status": 409, "error": "Conflict", "message": "Cannot cancel a ride that is already COMPLETED" }
```

---

## 8. `GET /actuator/health`

```json
{ "status": "UP" }
```

---

### Notes
- "Eligible driver" selection is currently done by the API client (see architecture diagram in `../report/diagrams/`) — it is not an automatic call from this service to driver-vehicle-service.
- See the [Postman collection](postman/ride-management-service.postman_collection.json) for a runnable end-to-end sequence covering the full lifecycle plus negative transitions.
