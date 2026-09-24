# account-service — API Endpoints

Base URL: `http://localhost:8081`
Auth: JWT (`Authorization: Bearer <token>`) — issued by this service; all four RideLink services validate it using the same shared secret.

| # | Method | Path | Auth | Purpose |
|---|--------|------|------|---------|
| 1 | POST | `/api/auth/register` | Public | Create a user (rider/driver/admin) and receive a JWT |
| 2 | POST | `/api/auth/login` | Public | Exchange email + password for a JWT |
| 3 | GET | `/api/users` | Bearer | List all users |
| 4 | GET | `/api/users/{id}` | Bearer | Get one user |
| 5 | PUT | `/api/users/{id}` | Bearer | Update a user's profile fields |
| 6 | PATCH | `/api/users/{id}/deactivate` | Bearer | Soft-disable a user |
| 7 | DELETE | `/api/users/{id}` | Bearer | Permanently remove a user |
| 8 | GET | `/actuator/health` | Public | Liveness check |

---

## 1. `POST /api/auth/register`

Creates a user and returns a JWT immediately (auto-login on register).

**Request**
```json
{
  "fullName": "Nimal Perera",
  "email": "rider@example.com",
  "password": "secret123",
  "phone": "+94771234567",
  "role": "RIDER"
}
```
`fullName`, `email`, `password` required. `password` min 8 characters. `role` optional, defaults to `RIDER` if omitted — valid values `RIDER`, `DRIVER`, `ADMIN`.

**Response `201 Created`**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": "665f0a1b2c3d4e5f6a7b8c9d",
    "fullName": "Nimal Perera",
    "email": "rider@example.com",
    "phone": "+94771234567",
    "role": "RIDER",
    "active": true,
    "createdAt": "2026-09-13T13:00:33Z"
  }
}
```

**Negative — `400 Bad Request`** (weak password)
```json
{
  "timestamp": "2026-09-13T13:00:33Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": ["password: Password must be at least 8 characters"]
}
```

```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Nimal Perera","email":"rider@example.com","password":"secret123","role":"RIDER"}'
```

---

## 2. `POST /api/auth/login`

**Request**
```json
{ "email": "rider@example.com", "password": "secret123" }
```

**Response `200 OK`** — same shape as register's response body.

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"rider@example.com","password":"secret123"}'
```

---

## 3. `GET /api/users`

**Response `200 OK`**
```json
[
  { "id": "665f0a1b2c3d4e5f6a7b8c9d", "fullName": "Nimal Perera", "email": "rider@example.com", "phone": "+94771234567", "role": "RIDER", "active": true, "createdAt": "2026-09-13T13:00:33Z" }
]
```

```bash
curl http://localhost:8081/api/users -H "Authorization: Bearer $TOKEN"
```

**Negative — `403 Forbidden`** (no/invalid token — note: this is Spring Security's default, not a custom `401`)
```
(empty body, HTTP 403)
```

---

## 4. `GET /api/users/{id}`

**Response `200 OK`** — single user object (see above).

**Negative — `404 Not Found`**
```json
{ "timestamp": "2026-09-13T13:00:33Z", "status": 404, "error": "Not Found", "message": "User not found with id: 9999" }
```

---

## 5. `PUT /api/users/{id}`

Partial update — every field optional, omitted fields are left unchanged. `role`, `password`, `active` are **not** editable here.

**Request**
```json
{ "fullName": "Nimal Perera Updated", "email": "nimal.updated@example.com", "phone": "+94770000000" }
```

**Response `200 OK`** — updated user object.

---

## 6. `PATCH /api/users/{id}/deactivate`

No body. Sets `active: false` without deleting the row.

**Response `200 OK`**
```json
{ "id": "665f0a1b2c3d4e5f6a7b8c9d", "fullName": "...", "email": "...", "active": false, "...": "..." }
```

---

## 7. `DELETE /api/users/{id}`

**Response `204 No Content`** — empty body.

---

## 8. `GET /actuator/health`

**Response `200 OK`**
```json
{ "status": "UP" }
```

---

### Notes
- All endpoints under `/api/auth/**` are public; everything under `/api/users/**` requires a valid Bearer token.
- Verified live against a running instance — see the [Postman collection](postman/account-service.postman_collection.json) for the full runnable sequence with auto-chaining variables.
