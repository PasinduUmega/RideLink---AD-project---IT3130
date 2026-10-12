# account-service

Handles user accounts, registration, login, and JWT issuance for the whole RideLink platform. Every other service trusts tokens signed here (they share `jwt.secret`).

- **Port:** `8081`
- **Database:** `ridelink_accounts` (MongoDB)
- **IDs:** `User.id` is a MongoDB ObjectId string (not numeric).

## Run locally

```bash
# requires a local MongoDB on localhost:27017,
# or override with env vars DB_HOST/DB_PORT/DB_NAME
mvn spring-boot:run
```

Tests use an embedded in-memory MongoDB automatically — no external database needed to run `mvn test`.

Or via Docker (see `shared/docker-compose.yml` to run the whole platform together).

## Environment variables

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | MongoDB host |
| `DB_PORT` | `27017` | MongoDB port |
| `DB_NAME` | `ridelink_accounts` | Database name |
| `DB_USERNAME` / `DB_PASSWORD` / `DB_AUTH_DATABASE` | (blank) / (blank) / `admin` | MongoDB credentials, if the instance requires auth |
| `JWT_SECRET` | (dev default in `application.yml`) | **Must match** across all 4 services |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | Token lifetime |

## API

### Auth

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | none | Create an account, returns a JWT |
| POST | `/api/auth/login` | none | Login, returns a JWT |

**Register**
```json
POST /api/auth/register
{
  "fullName": "Ada Lovelace",
  "email": "ada@example.com",
  "password": "supersecret",
  "phone": "+15551234567",
  "role": "RIDER"
}
```
`role` is optional and defaults to `RIDER`. Valid values: `RIDER`, `DRIVER`, `ADMIN`.

**Response (both register & login)**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": "665f0a1b2c3d4e5f6a7b8c9d",
    "fullName": "Ada Lovelace",
    "email": "ada@example.com",
    "phone": "+15551234567",
    "role": "RIDER",
    "active": true,
    "createdAt": "2026-09-13T04:00:00Z"
  }
}
```

Use the token on every subsequent request to any RideLink service:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Users (requires `Authorization: Bearer <token>`)

| Method | Path | Description |
|---|---|---|
| GET | `/api/users` | List all users |
| GET | `/api/users/{id}` | Get a user |
| PUT | `/api/users/{id}` | Update fullName/email/phone |
| PATCH | `/api/users/{id}/deactivate` | Deactivate an account |
| DELETE | `/api/users/{id}` | Delete a user |

## Notes / production TODOs

- Endpoint-level role checks (e.g. only `ADMIN` should list all users) are not yet enforced beyond "authenticated" — add `@PreAuthorize` checks as needed.
- The JWT secret is shared via a plain env var for simplicity; in production use a secrets manager and consider asymmetric (RS256) signing so only account-service holds the private key.
