# SOA Room Reservation

Hệ thống quản lý phòng học và đăng ký sử dụng phòng được xây theo hướng service-oriented.

## Services

| Component | Port | Responsibility |
|---|---:|---|
| Room Service | 8080 | Owns Room data and `room_db` |
| Reservation Service | 8081 | Verifies Room through REST; owns Reservation data and `reservation_db` |
| Audit Service | 8082 | Consumes `reservation.confirmed` asynchronously from RabbitMQ |
| Web UI | 5173 | Browser client; calls REST APIs only |
| RabbitMQ UI | 15672 | Local AMQP broker management UI |
| Room PostgreSQL | 5433 | `room_db` local development database |
| Reservation PostgreSQL | 5434 | `reservation_db` local development database |

## Architecture rule

```text
Reservation Service → REST → Room Service → room_db
Reservation Service → reservation_db
Reservation Service → RabbitMQ → Audit Service
```

Reservation Service must never read/write `room_db` directly. The Web UI must never connect to a database or RabbitMQ.

## Start local infrastructure

```powershell
Set-Location D:\soa-room-reservation
docker compose up -d
docker compose ps
```

Local development credentials are documented in `.env.example`. Do not commit a real `.env` file or production credentials.

## Build and start services

Open a separate PowerShell terminal for each command.

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

```powershell
Set-Location D:\soa-room-reservation\web-ui
npm ci
npm run dev
```

Open `http://localhost:5173`.

## API documentation

| Service | Swagger UI |
|---|---|
| Room | `http://localhost:8080/swagger-ui.html` |
| Reservation | `http://localhost:8081/swagger-ui.html` |

- v1 preserves TH1/TH2 response contracts.
- v2 provides filter/sort/pagination query APIs for TH3.

## Important limitation

Reservation persistence and RabbitMQ publishing are not a distributed transaction. If the broker is unavailable after a Reservation has been committed, the Reservation remains confirmed but the event may be missing. Transactional outbox is future work.
