# TH6 — Reservation ↔ Room integration test results

## Scope

Consumer: `reservation-service`.

Provider: `room-service`.

Supporting service: `auth-service` issues machine JWT; `audit-service` consumes the post-confirmation event.

## Collection execution order

Use `postman/reservation-room-integration-tests.json` in this exact order:

```text
I01 → I02 → I03 → I04 → I05 → I06
```

`I02` generates a future time slot; `I05` intentionally reuses it to test conflict.

## Kết quả chạy local

**Đã chạy:** I01–I06 được mở và chạy từng request trong Postman Collection trên môi trường local. Tất cả assertion của sáu request đều pass.

| Case | Integration behavior | Expected HTTP/result | Runtime result |
|---|---|---|---|
| I01 | Student authenticates before Consumer flow. | 200; `student01Token` stored. | **Đạt** — 200; token Student01 được lưu. |
| I02 | Reservation Consumer gets machine token, calls Room Provider, persists confirmed reservation and publishes Audit event. | 201; `CONFIRMED`; owner `STUDENT-001`. | **Đạt** — 201; `CONFIRMED`; owner được normalize về `STUDENT-001`. |
| I03 | Room Provider has no room id `999999`. | 404 `ROOM_NOT_FOUND`; no new Reservation. | **Đạt** — 404 `ROOM_NOT_FOUND`. |
| I04 | Provider returns Room that is not `AVAILABLE`. | 409 `ROOM_NOT_AVAILABLE`; no new Reservation. | **Đạt** — 409 `ROOM_NOT_AVAILABLE`. |
| I05 | Consumer detects same Room/time after Provider validation. | 409 `ROOM_SCHEDULE_CONFLICT`; no second Reservation. | **Đạt** — 409 `ROOM_SCHEDULE_CONFLICT`; duplicate slot bị từ chối. |
| I06 | No user JWT reaches Consumer. | 401 `AUTHENTICATION_REQUIRED`; no Provider call. | **Đạt** — 401 `AUTHENTICATION_REQUIRED`. |
| M01 | Provider unavailable: set `ROOM_SERVICE_BASE_URL=http://localhost:8099`, restart only Reservation Service, then repeat a valid create. | Retry then 503 `ROOM_SERVICE_UNAVAILABLE`; no new Reservation. | Chưa chạy trong bộ I01–I06; manual resilience case tùy chọn. |
| M02 | After I02 201, check Audit terminal. | `reservation.confirmed` received. | Chứng cứ terminal Audit được giữ riêng; không suy diễn chỉ từ status I02. |

## Kết luận I01–I06

**Đạt:** Consumer Reservation đã gọi Room Provider trong flow thành công, và các lỗi integration được trả theo contract có kiểm soát: 404 khi Room không tồn tại, 409 khi Room không available hoặc slot conflict, 401 khi request không có user JWT.

## Result rule

Không đánh dấu một case **Đạt** chỉ vì có status. Xác nhận cả status và business consequence:

- I02: `201` plus normalized owner and Audit event.
- I03/I04/I05/I06: no unexpected Reservation row is created.
- M01: Consumer does not persist a Reservation when Room Provider is unavailable.

## Current code trace

```text
ReservationService.createReservation
  → ReservationAccessPolicy.enforceCreateOwnership
  → RoomClient.getRoomById
  → ReservationPersistenceService.createConfirmedReservation
  → ReservationEventPublisher.publishReservationConfirmed
```
