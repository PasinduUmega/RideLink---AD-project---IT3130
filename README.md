# RideLink

A microservices-based ride-hailing platform built with **Java 17 + Spring Boot 3 + MongoDB**, split into four independently deployable services:

| Service | Port | Responsibility |
|---|---|---|
| [`account-service`](./account-service) | 8081 | User accounts, registration/login, JWT issuance |
| [`driver-vehicle-service`](./driver-vehicle-service) | 8082 | Driver profiles, approval status, vehicles |
| [`ride-management-service`](./ride-management-service) | 8083 | Ride requests, matching, trip lifecycle |
| [`fare-payment-service`](./fare-payment-service) | 8084 | Fare calculation, payment processing, refunds |

Each service owns its own MongoDB database (`ridelink_accounts`, `ridelink_drivers`, `ridelink_rides`, `ridelink_payments`) — no shared schema, no cross-service data access. All entity IDs are MongoDB ObjectId strings (not numeric) — see each service's README for details.

## Auth model

`account-service` is the only service that **issues** JWTs (HS256). The other three services **validate** tokens using the same shared secret (`jwt.secret` / `JWT_SECRET` env var) but never issue their own. Every protected endpoint expects:

```
Authorization: Bearer <token>
```

## Quick start (all services + databases)

```bash
cd shared
docker compose up --build
```

This starts 4 MongoDB containers and all 4 services, wired together with the shared JWT secret. Once up:

```bash
# Register a rider
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Ada Lovelace","email":"ada@example.com","password":"supersecret"}'

# Use the returned token against any service, e.g.:
curl http://localhost:8082/api/drivers \
  -H "Authorization: Bearer <token>"
```

## Running a single service locally

Each service is a standalone Maven project:

```bash
cd account-service
mvn spring-boot:run
```

By default it expects MongoDB on `localhost:27017` — override via `DB_HOST`, `DB_PORT`, `DB_NAME` env vars, or just point it at one of the `docker compose` databases.

## Structure

```
ridelink/
├── account-service/            # Spring Boot - users, auth, JWT
├── driver-vehicle-service/     # Spring Boot - drivers, vehicles
├── ride-management-service/    # Spring Boot - ride lifecycle
├── fare-payment-service/       # Spring Boot - fares, payments
├── shared/
│   ├── docker-compose.yml      # runs all 4 services + 4 MongoDB DBs
│   ├── postman/                # Postman collection & environment
│   └── docs/                   # architecture diagram, sequence diagram, OpenAPI contracts
└── .github/workflows/ci.yml    # matrix build: mvn clean verify per service
```

Each service's own README documents its full API, environment variables, and known production TODOs.

## Tech stack

- Java 17, Spring Boot 3.2 (Web, Data MongoDB, Security, Validation, Actuator)
- MongoDB 7
- JJWT for token signing/validation
- Lombok
- Maven
- Docker / Docker Compose

## CI

`.github/workflows/ci.yml` runs `mvn clean verify` for each service on every push/PR, using a build matrix so failures are isolated per service.

## Known gaps / next steps

- No API gateway yet — clients call each service directly on its own port. Consider adding one (Spring Cloud Gateway) for a single entry point, and moving JWT validation there.
- No role-based `@PreAuthorize` checks beyond "authenticated" — see each service's README for specifics.
- `ride-management-service` doesn't yet call `fare-payment-service` automatically on ride completion — that integration is stubbed and documented in its README.
- Payment gateway integration is simulated — see `fare-payment-service`'s README.
