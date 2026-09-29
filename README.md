# fare-payment-service

Calculates fares for completed rides and processes payments against them.

- **Port:** `8084`
- **Database:** `ridelink_payments` (MongoDB)
- **Auth:** validates JWTs issued by `account-service`.
- **IDs:** `Fare.id`, `Payment.id`, and cross-service references (`rideId`, `riderId`, `fareId`) are
  MongoDB ObjectId strings (not numeric).
# driver-vehicle-service

Manages driver profiles (license, approval status) and their vehicles.

- **Port:** `8082`
- **Database:** `ridelink_drivers` (MongoDB) — driver/vehicle profiles are self-contained documents
  with no need for cross-record joins or transactions.
- **Auth:** validates JWTs issued by `account-service` (shares `jwt.secret`); issues none itself.
- **IDs:** `Driver.id` / `Vehicle.id` are MongoDB ObjectId strings (not numeric).
# RideLink---AD-project---IT3130

## ride-management-service

Owns the lifecycle of a ride: request, driver acceptance, in-progress, completion, cancellation.

- **Port:** `8083`
- **Database:** `ridelink_rides` (MongoDB)
- **Auth:** validates JWTs issued by `account-service`.
- **IDs:** `Ride.id`, `riderId`, and `driverId` are MongoDB ObjectId strings (not numeric).

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
| `FARE_PER_MINUTE` | `0.20` | Per-minute rate |
| `FARE_MINIMUM` | `5.00` | Minimum fare |
| `FARE_AVG_SPEED_KMH` | `30` | Used to estimate duration when it is not supplied |
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

## API

All endpoints below require `Authorization: Bearer <token>` (a token issued by account-service).

### Drivers

| Method | Path | Description |
|---|---|---|
| POST | `/api/drivers` | Create a driver profile for a userId |
| GET | `/api/drivers` | List drivers (optional `?status=PENDING\|APPROVED\|REJECTED\|SUSPENDED`) |
| GET | `/api/drivers/{id}` | Get a driver |
| GET | `/api/drivers/by-user/{userId}` | Get a driver by their account-service userId |
| PUT | `/api/drivers/{id}` | Update license number/expiry |
| PATCH | `/api/drivers/{id}/status` | Change driver status (e.g. approve/reject) |
| DELETE | `/api/drivers/{id}` | Delete a driver |

**Create driver**
```json
POST /api/drivers
{
  "userId": "665f0a1b2c3d4e5f6a7b8c9d",
  "licenseNumber": "D1234567",
  "licenseExpiry": "2028-01-01"
}
```

**Update status**
```json
PATCH /api/drivers/665f1a2b3c4d5e6f7a8b9c0d/status
{ "status": "APPROVED" }
```

### Vehicles

| Method | Path | Description |
|---|---|---|
| POST | `/api/vehicles` | Register a vehicle to a driver |
| GET | `/api/vehicles` | List vehicles (optional `?driverId=`) |
| GET | `/api/vehicles/{id}` | Get a vehicle |
| PUT | `/api/vehicles/{id}` | Update make/model/year/color/capacity |
| PATCH | `/api/vehicles/{id}/deactivate` | Deactivate a vehicle |
| DELETE | `/api/vehicles/{id}` | Delete a vehicle |

**Register vehicle**
```json
POST /api/vehicles
{
  "driverId": "665f1a2b3c4d5e6f7a8b9c0d",
  "make": "Toyota",
  "model": "Camry",
  "year": 2022,
  "plateNumber": "ABC-1234",
  "color": "Silver",
  "capacity": 4
}
```

## Notes / production TODOs

- Cross-service references (`userId`) are not enforced by a foreign key since each service owns its own database — validate against account-service via HTTP call or an event stream if strict consistency is needed.
- Only `ADMIN`/ops roles should be able to call `PATCH /api/drivers/{id}/status`; add `@PreAuthorize("hasRole('ADMIN')")` once role policy is finalized.
| `FARE_SERVICE_URL` | `http://localhost:8084` | Base URL of fare-payment-service |

## Ride state machine

```
REQUESTED --accept--> ACCEPTED --start--> IN_PROGRESS --complete--> COMPLETED
    |                     |                    |
    +------------------ cancel ----------------+
                          v
                      CANCELLED
```
Each transition is validated server-side; calling e.g. `/start` on a ride that isn't `ACCEPTED` returns `409 Conflict`.

## API

All endpoints require `Authorization: Bearer <token>`.

| Method | Path | Description |
|---|---|---|
| POST | `/api/rides` | Rider requests a ride |
| GET | `/api/rides` | List rides (optional `?riderId=`, `?driverId=`, or `?status=`) |
| GET | `/api/rides/{id}` | Get a ride |
| PATCH | `/api/rides/{id}/accept` | Driver accepts (`{"driverId": "665f1a2b3c4d5e6f7a8b9c0d"}` — a MongoDB ObjectId from driver-vehicle-service) |
| PATCH | `/api/rides/{id}/start` | Driver starts the trip |
| PATCH | `/api/rides/{id}/complete` | Driver marks the trip complete |
| PATCH | `/api/rides/{id}/cancel` | Rider or driver cancels (`{"reason": "..."}`, optional body) |

**Request a ride**
```json
POST /api/rides
{
  "riderId": "665f0a1b2c3d4e5f6a7b8c9d",
  "pickupLocation": "123 Main St",
  "dropoffLocation": "456 Market St",
  "pickupLat": 37.7749,
  "pickupLng": -122.4194,
  "dropoffLat": 37.7849,
  "dropoffLng": -122.4094,
  "distanceKm": 4.2
}
```
If `distanceKm` is provided, an `estimatedFare` is calculated with a simple flat-rate model (`$2.50` base + `$1.20`/km) — this is a placeholder; final billing happens in `fare-payment-service`.

## Notes / production TODOs

- On `complete`, this service currently only recalculates its own `estimatedFare`. A production build would call `fare-payment-service`'s `POST /api/fares` (see `services.fare-payment.base-url`) to create the authoritative fare record — wire that up in `RideService.complete()`.
- Driver eligibility (must be `APPROVED` in driver-vehicle-service) isn't checked before `accept` — add a call to driver-vehicle-service or an event-driven check.
- IT24102952
