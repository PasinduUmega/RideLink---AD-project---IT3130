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
