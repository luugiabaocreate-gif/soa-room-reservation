# SOA Room Reservation — TH1 to TH5

Hệ thống quản lý phòng học và đăng ký sử dụng phòng được xây theo hướng service-oriented. TH5 bổ sung Auth Service, Spring Security, BCrypt và JWT để bảo vệ API bằng role và quyền sở hữu resource.

## Components and ownership

| Component | Port | Ownership/responsibility |
|---|---:|---|
| Auth Service | 8083 | Owns `auth_db`; authenticates local users, issues 15-minute JWT and service JWT. |
| Room Service | 8080 | Owns `room_db`; room query needs JWT; room mutation needs `ROOM_MANAGER` or `ADMIN`. |
| Reservation Service | 8081 | Owns `reservation_db`; Student can only create/read own reservations. |
| Audit Service | 8082 | Consumes `reservation.confirmed` from RabbitMQ. |
| Web UI | 5173 | Login form; sends Bearer JWT to REST API; never accesses DB/RabbitMQ. |
| RabbitMQ Management | 15672 | Local broker management UI. |
| Room/Reservation/Auth PostgreSQL | 5433 / 5434 / 5435 | Isolated data stores. |

```text
Browser -- username/password --> Auth Service -- JWT --> Room / Reservation API
Reservation Service -- client credential --> Auth Service -- service JWT --> Room Service
Reservation Service -- event --> RabbitMQ --> Audit Service
```

Reservation Service **does not forward the user JWT** to Room Service. It obtains its own short-lived `SERVICE_RESERVATION` token with audience `room-service`.

## First-time local secret setup

```powershell
Set-Location D:\soa-room-reservation
Copy-Item .env.example .env
```

Generate a Base64 JWT signing key and a long random client secret, then replace the two `REPLACE_...` values in `.env`:

```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

`.env` is ignored by Git. Do not commit a real signing key, production password or service credential.

## Start infrastructure

```powershell
Set-Location D:\soa-room-reservation
docker compose up -d
docker compose ps
```

Wait until `soa-rabbitmq`, `soa-room-db`, `soa-reservation-db`, and `soa-auth-db` are all `healthy`.

## Build and run services

Open one PowerShell per Java service. Before each service, load `.env` into that terminal:

```powershell
Set-Location D:\soa-room-reservation
. .\scripts\Import-LocalEnv.ps1
```

Then run services in this order:

```powershell
Set-Location D:\soa-room-reservation\auth-service
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

```powershell
Set-Location D:\soa-room-reservation\room-service
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

```powershell
Set-Location D:\soa-room-reservation\reservation-service
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

```powershell
Set-Location D:\soa-room-reservation\audit-service
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Web UI does not need the JVM secret environment variables:

```powershell
Set-Location D:\soa-room-reservation\web-ui
npm ci
npm run dev
```

Open `http://localhost:5173`.

## Local demo accounts

All are local-only and have password `DemoPassword123!`.

| Username | Role | Meaning |
|---|---|---|
| `student01` | STUDENT | Can read rooms and only create/read reservations owned by `STUDENT-001`. |
| `student02` | STUDENT | Can only create/read resources owned by `STUDENT-002`. |
| `roommanager01` | ROOM_MANAGER | Can mutate rooms and inspect reservation schedule. |
| `admin01` | ADMIN | Administrative cross-resource access. |

## API documentation

| Service | Swagger UI |
|---|---|
| Auth | `http://localhost:8083/swagger-ui.html` |
| Room | `http://localhost:8080/swagger-ui.html` |
| Reservation | `http://localhost:8081/swagger-ui.html` |

Read `docs/th5-security-design.md` for trust boundaries and `docs/th5-security-test.md` for the required 401/403/ownership evidence. The importable Postman collection is in `postman/TH5_Security.postman_collection.json`.

## Security limitation and future work

This is a teaching/local implementation. Production should use HTTPS, secret manager, asymmetric signing key/JWKS and rotation, OIDC/OAuth 2.0, refresh-token policy, rate limiting, persistent security audit, and preferably mTLS or OAuth client credentials managed by an identity provider. The current local client credential demonstrates the rule that service-to-service calls do not reuse arbitrary user tokens.
