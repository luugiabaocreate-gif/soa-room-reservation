# TH4 — Persistence test checklist

## Start infrastructure

```powershell
Set-Location D:\soa-room-reservation
docker compose up -d
docker compose ps
```

Expected running/healthy containers:

```text
soa-rabbitmq
soa-room-db
soa-reservation-db
```

## Required evidence

| Case | Action | Expected result |
|---|---|---|
| Room seed | Start Room Service against PostgreSQL | Five seed rooms are inserted only when `rooms` is empty. |
| Room persistence | POST a Room, restart Room Service, GET it | Same Room remains after restart. |
| Reservation persistence | Create valid Reservation, restart Reservation Service, reload UI/API list | Same Reservation remains after restart. |
| Room missing | POST Reservation with unknown room id | 404; no row is inserted into `reservation_db`; no event. |
| Conflict | POST overlapping Reservation | 409; no new reservation row; no event. |
| Room unavailable | Stop Room Service, then POST valid Reservation | 503 after retry; no row is inserted. |
| Async success | Valid Reservation | One event reaches Audit Service. |

## Useful database inspection commands

```powershell
# Inspect Room database

docker exec -it soa-room-db psql -U room_user -d room_db -c "SELECT id, code, capacity, status FROM rooms ORDER BY id;"

# Inspect Reservation database

docker exec -it soa-reservation-db psql -U reservation_user -d reservation_db -c "SELECT id, room_id, room_code, requester_id, start_time, end_time, status FROM reservations ORDER BY id;"
```

> `docker compose down -v` removes local database volumes and therefore removes test data. Use it only when the team intentionally wants a fresh database.
