# API SECURITY DESIGN
## SOA Room Reservation — TH5

## 1. Luồng bảo mật

```text
Browser
  → POST /api/v1/auth/login
  → Auth Service: BCrypt matches
  → User JWT: role + requesterId + audience + expiry
  → Room/Reservation JWT filter + verifier
  → SecurityConfig role rule
  → ReservationAccessPolicy ownership rule
```

Reservation Service gọi Room bằng machine JWT riêng, không forward JWT Browser:

```text
Reservation → POST /api/v1/auth/service-token → machine JWT
Reservation → GET Room + machine JWT → Room Service
```

## 2. API và contract bảo mật

| API | Credential | Rule | Kết quả đã chạy |
|---|---|---|---|
| `POST /api/v1/auth/login` | username/password | BCrypt + active account | 200 đúng credential; 401 `INVALID_CREDENTIALS` sai password. |
| `GET /api/v2/rooms` | User JWT | authenticated | No token 401; Student token 200. |
| `POST /api/v1/rooms` | User JWT | `ROOM_MANAGER` hoặc `ADMIN` | Student 403 `ACCESS_DENIED`; Manager 201. |
| `POST /api/v1/reservations` | Student/Admin JWT | Student identity lấy từ JWT | Spoof requesterId bị ghi đè về Student01. |
| `GET /api/v2/reservations` | User JWT | Student query bị ép owner claim | Student01 không đọc Student02; Student02 không đọc Student01. |

## 3. JWT contract

| Claim | Mục đích |
|---|---|
| `sub` | username/account identity. |
| `roles` | Rule role ở SecurityConfig. |
| `requesterId`, `displayName` | Ownership Reservation cho Student. |
| `audiences` | Token chỉ được service đích chấp nhận. |
| `iss`, `exp` | Kiểm issuer và thời hạn 900 giây. |

## 4. Error contract

| Tình huống | HTTP | Code |
|---|---:|---|
| Không có / token invalid | 401 | `AUTHENTICATION_REQUIRED` |
| Login credential sai | 401 | `INVALID_CREDENTIALS` |
| Token hợp lệ nhưng role thiếu | 403 | `ACCESS_DENIED` |

## 5. Source trace

```text
AuthService → JwtTokenService
JwtAuthenticationFilter → JwtTokenVerifier
SecurityConfig → SecurityErrorResponseWriter
ReservationAccessPolicy → ReservationService
```

**Đạt:** contract trên đã chạy bằng Postman manual và Collection Runner; kết quả từng case nằm trong `test-results.md`.
