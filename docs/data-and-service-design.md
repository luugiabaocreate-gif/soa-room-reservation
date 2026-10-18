# TH4 — Data ownership và service design

## 1. Database ownership

| Data | Owner service | Database | Cách service khác lấy data |
|---|---|---|---|
| Room | Room Service | `room_db` | `GET /api/v1/rooms/{id}` qua REST/JSON |
| Reservation | Reservation Service | `reservation_db` | Reservation API của chính service |
| Audit log | Audit Service (future scope) | Chưa triển khai DB | Consume `reservation.confirmed` từ RabbitMQ |

## 2. Logical ERD

```text
room_db
└── rooms
    ├── id (PK)
    ├── code (UNIQUE)
    ├── building, floor, type, capacity
    ├── total_computers, working_computers, broken_computers
    └── status

reservation_db
└── reservations
    ├── id (PK)
    ├── room_id          ← external reference, NOT a physical cross-DB foreign key
    ├── room_code        ← snapshot returned by Room Service
    ├── requester_id, requester_name, purpose
    ├── start_time, end_time, attendee_count
    ├── status, created_at
    └── index(room_id, status, start_time, end_time)
```

## 3. Create Reservation sequence

```text
Browser/Postman
  → POST /api/v1/reservations
  → Reservation Service validates request/time
  → GET Room Service /api/v1/rooms/{id}
  → Room Service reads room_db and returns JSON
  → Reservation Service validates status/capacity
  → local transaction in reservation_db:
       check overlap → save Reservation CONFIRMED → commit
  → publish reservation.confirmed event to RabbitMQ
  → Audit Service logs the received event
```

## 4. Transaction boundary and limitation

`@Transactional` in `ReservationPersistenceService` protects only conflict-check and save in `reservation_db`.

It does **not** make Room REST verification or RabbitMQ publish part of one distributed transaction. If RabbitMQ publish fails after database commit, Reservation remains confirmed but its event can be missing. Transactional outbox/idempotent audit persistence are intentionally future work, not falsely claimed as completed.
