# driver-vehicle-service

Manages driver profiles (license, approval status) and their vehicles.

- **Port:** `8082`
- **Database:** `ridelink_drivers` (MongoDB) — driver/vehicle profiles are self-contained documents
  with no need for cross-record joins or transactions.
- **Auth:** validates JWTs issued by `account-service` (shares `jwt.secret`); issues none itself.
- **IDs:** `Driver.id` / `Vehicle.id` are MongoDB ObjectId strings (not numeric).

## Run locally

```bash
mvn spring-boot:run
```

Uses `MONGODB_URI` for the MongoDB connection (defaults to `mongodb://localhost:27017/ridelink_drivers`).
For local development, run from this directory to load the optional, git-ignored
`mongodb-local.properties` file containing `MONGODB_URI=<connection string>`.
The `MONGODB_URI` environment variable overrides that local file.
Tests use an embedded in-memory MongoDB automatically — no external database needed to run `mvn test`.

## Environment variables

| Variable | Default | Description |
|---|---|---|
| `MONGODB_URI` | `mongodb://localhost:27017/ridelink_drivers` | Full MongoDB connection URI including credentials and database name |
| `JWT_SECRET` | (dev default) | Must match account-service |

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
