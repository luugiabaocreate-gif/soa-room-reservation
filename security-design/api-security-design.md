# API security design

## Authentication

- `POST /api/v1/auth/login`: username/password over local HTTP only for classroom demo; production requires HTTPS.
- Auth Service returns a 900-second signed JWT with issuer, subject, roles, audiences, requesterId and displayName.
- Room/Reservation parse and verify token locally; they do not call Auth Service on every browser request.

## Authorization

- Room mutation endpoints are role guarded.
- Reservation creation/listing are role guarded and use ownership checks at business-service layer.
- HTTP 401 means missing/invalid/expired/wrong-audience JWT; 403 means token was valid but permission was insufficient.

## Internal API

Reservation Service posts clientId/clientSecret to `POST /api/v1/auth/service-token`; Auth returns a short-lived machine JWT with role `SERVICE_RESERVATION` and audience `room-service`. Reservation Service sends that machine JWT for `GET /api/v1/rooms/{id}`. It does not forward the end-user JWT.
