# TH6 — API contract: Reservation Consumer và Room Provider

## Provider: Room Service

### Lookup Room cho integration

```http
GET http://localhost:8080/api/v1/rooms/{id}
Authorization: Bearer <machine JWT issued to reservation-service>
```

Response consumer cần dùng:

```json
{
  "id": 1,
  "code": "A101",
  "capacity": 60,
  "status": "AVAILABLE"
}
```

`Reservation Service` chỉ trust các trường `id`, `code`, `capacity`, `status` trong `RoomResponse`; không truy cập `room_db`.

## Consumer API: Reservation Service

### Create confirmed Reservation

```http
POST http://localhost:8081/api/v1/reservations
Authorization: Bearer <user JWT>
Content-Type: application/json
```

```json
{
  "roomId": 1,
  "requesterId": "STUDENT-002",
  "requesterName": "Spoofed name",
  "purpose": "TH6 integration flow",
  "startTime": "2026-12-20T09:00:00",
  "endTime": "2026-12-20T10:00:00",
  "attendeeCount": 20
}
```

Nếu caller là `student01`, body identity bị normalized trước khi gọi Provider:

```json
{
  "requesterId": "STUDENT-001",
  "requesterName": "Student One"
}
```

### Success response

```http
201 Created
```

```json
{
  "id": 101,
  "roomId": 1,
  "roomCode": "A101",
  "requesterId": "STUDENT-001",
  "requesterName": "Student One",
  "purpose": "TH6 integration flow",
  "startTime": "2026-12-20T09:00:00",
  "endTime": "2026-12-20T10:00:00",
  "attendeeCount": 20,
  "status": "CONFIRMED"
}
```

## Error contract

### Room provider cannot find Room

```http
404 Not Found
```

```json
{
  "code": "ROOM_NOT_FOUND",
  "status": 404
}
```

### Provider Room returns data but Room is unavailable or capacity is insufficient

```http
409 Conflict
```

```json
{
  "code": "ROOM_NOT_AVAILABLE",
  "status": 409
}
```

### Consumer detects an overlapping confirmed Reservation

```http
409 Conflict
```

```json
{
  "code": "ROOM_SCHEDULE_CONFLICT",
  "status": 409
}
```

### Provider unavailable after retry

```http
503 Service Unavailable
```

```json
{
  "code": "ROOM_SERVICE_UNAVAILABLE",
  "status": 503
}
```

### Authentication/authorization

```text
401 AUTHENTICATION_REQUIRED  → caller has no usable Bearer identity
403 ACCESS_DENIED            → caller identity is valid but role/policy denies action
```
