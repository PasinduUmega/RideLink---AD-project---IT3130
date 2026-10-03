# driver-vehicle-service — API Endpoints

Base URL: `http://localhost:8082`
Auth: JWT (`Authorization: Bearer <token>`) — this service **validates only**; it never issues tokens. Get a token from `account-service`'s `/api/auth/login` or `/api/auth/register`.

| # | Method | Path | Auth | Purpose |
|---|--------|------|------|---------|
| 1 | POST | `/api/drivers` | Bearer | Create a driver profile for an existing account-service user |
| 2 | GET | `/api/drivers` | Bearer | List drivers (optionally filter by `status`) |
| 3 | GET | `/api/drivers/{id}` | Bearer | Get one driver (with their vehicles) |
| 4 | GET | `/api/drivers/by-user/{userId}` | Bearer | Look up a driver profile by account-service `userId` |
| 5 | PUT | `/api/drivers/{id}` | Bearer | Update license details / service area |
| 6 | PATCH | `/api/drivers/{id}/status` | Bearer | Approve / reject / suspend a driver |
| 7 | DELETE | `/api/drivers/{id}` | Bearer | Remove a driver (cascades to their vehicles) |
| 8 | POST | `/api/vehicles` | Bearer | Register a vehicle under a driver |
| 9 | GET | `/api/vehicles` | Bearer | List vehicles (optionally filter by `driverId`) |
| 10 | GET | `/api/vehicles/{id}` | Bearer | Get one vehicle |
| 11 | PUT | `/api/vehicles/{id}` | Bearer | Update vehicle details |
| 12 | PATCH | `/api/vehicles/{id}/deactivate` | Bearer | Soft-disable a vehicle |
| 13 | DELETE | `/api/vehicles/{id}` | Bearer | Remove a vehicle |
| 14 | GET | `/api/drivers/available` | Bearer | Eligible available drivers (area / radius / seats) |
| 15 | PATCH | `/api/drivers/{id}/availability` | Bearer | Set OFFLINE / AVAILABLE / ON_TRIP |
| 16 | PUT | `/api/drivers/{id}/location` | Bearer | Set current location |
| 17 | POST | `/api/drivers/{id}/location/simulate` | Bearer | Simulate movement (random step <= 1 km) |
| 18 | GET | `/actuator/health` | Public | Liveness check |

---

## Drivers

### 1. `POST /api/drivers`

**Request**
```json
{ "userId": "665f0a1b2c3d4e5f6a7b8c9d", "licenseNumber": "DL-2026-00123", "licenseExpiry": "2028-12-31" }
```
`userId`, `licenseNumber` required. `licenseExpiry` must be a future date if provided. `licenseNumber` must be unique; a `userId` can only have one driver profile. New drivers start `PENDING`.

**Response `201 Created`**
```json
{
  "id": "665f1a2b3c4d5e6f7a8b9c0d", "userId": "665f0a1b2c3d4e5f6a7b8c9d", "licenseNumber": "DL-2026-00123", "licenseExpiry": "2028-12-31",
  "status": "PENDING", "rating": 5.0, "vehicles": [], "createdAt": "2026-09-13T15:00:00Z"
}
```

**Negative — `400 Bad Request`**
```json
{ "timestamp": "2026-09-13T15:00:00Z", "status": 400, "error": "Bad Request", "message": "This user already has a driver profile" }
```

```bash
curl -X POST http://localhost:8082/api/drivers -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"userId":"665f0a1b2c3d4e5f6a7b8c9d","licenseNumber":"DL-2026-00123","licenseExpiry":"2028-12-31"}'
```

### 2. `GET /api/drivers?status=APPROVED`

Only-eligible-drivers query used by the ride-matching workflow. Omit `status` to list every driver.

**Response `200 OK`**
```json
[{ "id": "665f1a2b3c4d5e6f7a8b9c0d", "userId": "665f0a1b2c3d4e5f6a7b8c9d", "licenseNumber": "DL-2026-00123", "status": "APPROVED", "rating": 5.0, "vehicles": [], "createdAt": "2026-09-13T15:00:00Z" }]
```

### 3. `GET /api/drivers/{id}`

**Response `200 OK`** — includes nested `vehicles[]`.
**Negative — `404 Not Found`**
```json
{ "timestamp": "2026-09-13T15:00:00Z", "status": 404, "error": "Not Found", "message": "Driver not found with id: 000000000000000000000000" }
```

### 4. `GET /api/drivers/by-user/{userId}`

**Response `200 OK`** — single driver object. `404` if that user has no driver profile.

### 5. `PUT /api/drivers/{id}`

**Request** (both fields optional)
```json
{ "licenseNumber": "DL-2026-00123", "licenseExpiry": "2029-06-30" }
```
`status` and `rating` are **not** editable here.

### 6. `PATCH /api/drivers/{id}/status`

**Request**
```json
{ "status": "APPROVED" }
```
Valid values: `PENDING`, `APPROVED`, `REJECTED`, `SUSPENDED`. Only `APPROVED` drivers are eligible for ride matching.

**Negative — `400 Bad Request`** (missing status)
```json
{ "timestamp": "2026-09-13T15:00:00Z", "status": 400, "error": "Bad Request", "message": "Validation failed", "details": ["status: must not be null"] }
```

### 7. `DELETE /api/drivers/{id}`

**Response `204 No Content`**

---

## Vehicles

### 8. `POST /api/vehicles`

**Request**
```json
{ "driverId": "665f1a2b3c4d5e6f7a8b9c0d", "make": "Toyota", "model": "Prius", "year": 2019, "plateNumber": "WP-CAB-4521", "color": "Silver", "capacity": 4 }
```
`driverId`, `make`, `model`, `plateNumber` required. `plateNumber` unique. `capacity` defaults to 4 if omitted, must be positive if sent.

**Response `201 Created`**
```json
{
  "id": "77a0b1c2d3e4f5a6b7c8d9e0", "driverId": "665f1a2b3c4d5e6f7a8b9c0d", "make": "Toyota", "model": "Prius", "year": 2019,
  "plateNumber": "WP-CAB-4521", "color": "Silver", "capacity": 4, "active": true,
  "createdAt": "2026-09-13T15:05:00Z"
}
```

**Negative — `404 Not Found`** (bad driverId)
```json
{ "timestamp": "2026-09-13T15:05:00Z", "status": 404, "error": "Not Found", "message": "Driver not found with id: 000000000000000000000000" }
```

**Negative — `400 Bad Request`** (duplicate plate)
```json
{ "timestamp": "2026-09-13T15:05:00Z", "status": 400, "error": "Bad Request", "message": "A vehicle with this plate number already exists" }
```

### 9. `GET /api/vehicles?driverId={id}`

Omit `driverId` to list every vehicle.

### 10. `GET /api/vehicles/{id}`

`404` if not found.

### 11. `PUT /api/vehicles/{id}`

**Request** (all fields optional)
```json
{ "color": "White", "capacity": 5 }
```
`plateNumber` and `driverId` are **not** editable here.

### 12. `PATCH /api/vehicles/{id}/deactivate`

No body. Sets `active: false`.

### 13. `DELETE /api/vehicles/{id}`

**Response `204 No Content`**

### 14. `GET /actuator/health`

```json
{ "status": "UP" }
```

---

### Notes
- Every `/api/drivers/**` and `/api/vehicles/**` request requires the shared JWT — obtain it from `account-service`, not this service.
- See the [Postman collection](postman/driver-vehicle-service.postman_collection.json) for a runnable end-to-end sequence.

---

## Availability, service area & simulated location

Drivers now carry an operational **availability** separate from their approval `status`:
`OFFLINE` (default) | `AVAILABLE` | `ON_TRIP`. They also have a `serviceArea` (e.g. `"Colombo"`, set on create/update)
and a simulated current location (`currentLat`, `currentLng`, `locationUpdatedAt`).

| Method | Path | Purpose |
|--------|------|---------|
| PATCH | `/api/drivers/{id}/availability` | Set `OFFLINE` / `AVAILABLE` / `ON_TRIP` |
| PUT | `/api/drivers/{id}/location` | Set current location manually (`lat`, `lng`) |
| POST | `/api/drivers/{id}/location/simulate` | Move driver to a random point within 1 km (no body) |
| GET | `/api/drivers/available` | Eligible drivers for a ride |

### Rules
- Going `AVAILABLE` requires `status = APPROVED` **and** at least one active vehicle, otherwise `400`.
- Changing `status` to anything other than `APPROVED` forces availability to `OFFLINE`.
- `ON_TRIP` is normally set by ride-management-service when a driver is assigned, and reset to `AVAILABLE` when the ride completes or is cancelled.
- Simulation starts from the driver's current location, or from the centre of their service area if they have none
  (known areas: Colombo, Kandy, Galle, Negombo, Jaffna, Kurunegala, Matara). Unknown area with no location -> `400`.

### `PATCH /api/drivers/{id}/availability`
```json
{ "availability": "AVAILABLE" }
```

### `PUT /api/drivers/{id}/location`
```json
{ "lat": 6.9271, "lng": 79.8612 }
```

### `GET /api/drivers/available?serviceArea=Colombo&lat=6.93&lng=79.85&radiusKm=10&minCapacity=3`

All parameters optional. A driver is **eligible** when:
1. `status = APPROVED` and `availability = AVAILABLE`,
2. they own an active vehicle with `capacity >= minCapacity` (default 1),
3. `serviceArea` matches (case-insensitive) if given,
4. if `lat`/`lng` are given: they have a location within `radiusKm` (default 10 km, haversine distance).

With a pickup point the list is sorted nearest-first and each entry includes `distanceKm`.
Passing only one of `lat`/`lng` -> `400`.
