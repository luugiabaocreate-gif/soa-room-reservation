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

## Expected and recordable results

| Case | Integration behavior | Expected HTTP/result | Runtime result |
|---|---|---|---|
| I01 | Student authenticates before Consumer flow. | 200; `student01Token` stored. | Run with local TH6 environment. |
| I02 | Reservation Consumer gets machine token, calls Room Provider, persists confirmed reservation and publishes Audit event. | 201; `CONFIRMED`; owner `STUDENT-001`. | Run with local TH6 environment. |
| I03 | Room Provider has no room id `999999`. | 404 `ROOM_NOT_FOUND`; no new Reservation. | Run with local TH6 environment. |
| I04 | Provider returns Room that is not `AVAILABLE`. | 409 `ROOM_NOT_AVAILABLE`; no new Reservation. | Run with local TH6 environment. |
| I05 | Consumer detects same Room/time after Provider validation. | 409 `ROOM_SCHEDULE_CONFLICT`; no second Reservation. | Run with local TH6 environment. |
| I06 | No user JWT reaches Consumer. | 401 `AUTHENTICATION_REQUIRED`; no Provider call. | Run with local TH6 environment. |
| M01 | Provider unavailable: set `ROOM_SERVICE_BASE_URL=http://localhost:8099`, restart only Reservation Service, then repeat a valid create. | Retry then 503 `ROOM_SERVICE_UNAVAILABLE`; no new Reservation. | Manual resilience test. |
| M02 | After I02 201, check Audit terminal. | `reservation.confirmed` received. | Manual terminal evidence. |

## Result rule

Do not mark a case **Đạt** merely because a request returns a status. Confirm both the status and the business consequence:

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
