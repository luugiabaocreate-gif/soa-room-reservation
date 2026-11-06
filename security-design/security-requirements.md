# SECURITY REQUIREMENTS
## SOA Room Reservation — TH5

| ID | Yêu cầu | Cách triển khai | Source | Kết quả |
|---|---|---|---|---|
| SR-01 | Password không lưu plain text. | BCrypt encode/matches. | `AuthDataInitializer`, `AuthService`, `AuthUserEntity`. | **Đạt** — login đúng 200, sai password 401. |
| SR-02 | Protected API cần JWT hợp lệ. | Bearer filter; verify signature, issuer, audience, role. | `JwtAuthenticationFilter`, `JwtTokenVerifier`. | **Đạt** — no token 401. |
| SR-03 | Role bảo vệ Room mutation. | POST/PUT/DELETE Room chỉ Manager/Admin. | Room `SecurityConfig`. | **Đạt** — Student 403, Manager 201. |
| SR-04 | Student không giả owner khi create Reservation. | Server ghi đè requesterId/name bằng JWT claims. | `ReservationAccessPolicy.enforceCreateOwnership`. | **Đạt** — spoof body vẫn owner Student01. |
| SR-05 | Student không đọc data owner khác. | Server ép requesterId filter về claim JWT. | `ReservationAccessPolicy.effectiveRequesterIdForRead`. | **Đạt** — foreign filter và Student02 isolation pass. |
| SR-06 | Reservation gọi Room bằng service identity. | Client credential cấp machine JWT audience room-service. | `ServiceTokenClient`, `RoomClient`, `JwtTokenService`. | **Đạt** — machine token lookup hoạt động. |
| SR-07 | Secret không nằm trong source/Git/video. | `.env`, `.gitignore`, không log full JWT/secret. | `.env.example`, runbook. | **Đạt** — video chỉ hiện status/response cần thiết. |

## Error rule

| Case | HTTP | Code | Kết quả |
|---|---:|---|---|
| No/invalid Bearer | 401 | `AUTHENTICATION_REQUIRED` | **Đạt** |
| Wrong password | 401 | `INVALID_CREDENTIALS` | **Đạt** |
| Valid JWT, role thiếu | 403 | `ACCESS_DENIED` | **Đạt** |

## Giới hạn hiện tại

- HMAC shared secret, không phải JWKS/key rotation production.
- Chưa có refresh token, rate limiting, TLS/mTLS, audit persistence/search hoàn chỉnh.
- Các giới hạn này không ảnh hưởng các case bảo mật KT1 đã chạy.
