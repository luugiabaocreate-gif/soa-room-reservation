# TH5 — Authentication, Authorization và API Security

## 1. Phạm vi áp dụng cho SOA Room Reservation

TH5 bổ sung `auth-service` (port `8083`) và database riêng `auth_db` (PostgreSQL host port `5435`). Auth Service là issuer JWT; Room Service và Reservation Service là resource service độc lập tự xác minh JWT. Audit Service chỉ consume RabbitMQ, không mở business REST API cho browser.

```text
Browser/Web UI -- username/password --> Auth Service -- JWT 15 phút --> Browser/Web UI
Browser/Web UI -- Bearer JWT --> Room Service / Reservation Service
Reservation Service -- client credential --> Auth Service -- service JWT --> Room Service
Reservation Service -- service JWT --> Room Service
```

**Không chuyển tiếp JWT của sinh viên từ Reservation Service sang Room Service.** Reservation Service xin machine token riêng bằng client credential; token đó chỉ có `SERVICE_RESERVATION` và audience `room-service`.

## 2. Tài sản cần bảo vệ

| Tài sản/chức năng | Owner | Rủi ro chính | Cách bảo vệ |
|---|---|---|---|
| Room data và thay đổi Room | Room Service / `room_db` | Sinh viên sửa/xóa phòng | JWT + role `ROOM_MANAGER`/`ADMIN` cho POST/PUT/DELETE |
| Reservation và requesterId | Reservation Service / `reservation_db` | Người dùng xem/tạo đăng ký của người khác | JWT + role + owner check bằng requesterId trong token |
| Username/password hash | Auth Service / `auth_db` | Lộ mật khẩu | BCrypt; không lưu password plain text |
| JWT signing key | Environment `.env` local | Giả mạo token | Base64 secret không commit Git; JWT có issuer, expiry và audience |
| REST Reservation → Room | Internal service boundary | Gọi Room API bằng token người dùng tùy ý | Client credential cấp service JWT audience riêng |

## 3. Ma trận role/permission

| API / hành động | STUDENT | ROOM_MANAGER | ADMIN | SERVICE_RESERVATION |
|---|---:|---:|---:|---:|
| POST `/api/v1/auth/login` | Public | Public | Public | Public |
| GET Room v1/v2 | Có JWT | Có JWT | Có JWT | Có (chỉ lookup nội bộ) |
| POST/PUT/DELETE Room | Không | Có | Có | Không |
| POST Reservation | Chỉ tạo cho chính mình | Không | Có thể tạo thay | Không |
| GET Reservation v1/v2 | Chỉ thấy reservation của mình | Xem tất cả | Xem tất cả | Không |
| Reservation → Room lookup | Không trực tiếp | Không trực tiếp | Không trực tiếp | Có token nội bộ riêng |

`STUDENT` không tin requesterId/requesterName do browser gửi. Reservation Service ghi requesterId/displayName từ JWT vào request đã chuẩn hóa. Khi STUDENT gửi filter requesterId khác, Service luôn thay bằng requesterId trong JWT.

## 4. Local demo accounts

Password chung, chỉ cho local/demo: `DemoPassword123!`.

| Username | Role | requesterId |
|---|---|---|
| `student01` | STUDENT | `STUDENT-001` |
| `student02` | STUDENT | `STUDENT-002` |
| `roommanager01` | ROOM_MANAGER | — |
| `admin01` | ADMIN | — |

Các account được AuthDataInitializer seed vào `auth_db` bằng BCrypt khi chưa tồn tại; password plain text không được lưu database.

## 5. JWT contract

JWT được ký HMAC bằng `JWT_SECRET`, issuer `soa-room-reservation-auth`, expiry 900 seconds. Claims business:

```json
{
  "sub": "student01",
  "displayName": "Student One",
  "requesterId": "STUDENT-001",
  "roles": ["STUDENT"],
  "audiences": ["room-service", "reservation-service"]
}
```

Room/Reservation Service từ chối token hết hạn, sai chữ ký, sai issuer, không có roles hoặc không có audience của service đích bằng HTTP 401. Token hợp lệ nhưng role không phù hợp trả HTTP 403.

## 6. Quy tắc HTTP error

| Tình huống | HTTP | code |
|---|---:|---|
| Không gửi Bearer token tới API bảo vệ | 401 | `AUTHENTICATION_REQUIRED` |
| JWT sai/hết hạn/sai audience | 401 | `AUTHENTICATION_REQUIRED` |
| JWT đúng nhưng role không được phép | 403 | `ACCESS_DENIED` |
| Login sai username/password hoặc user inactive | 401 | `INVALID_CREDENTIALS` |
| Resource Room không tồn tại | 404 | `ROOM_NOT_FOUND` |
| Request validation sai | 400 | `VALIDATION_ERROR` |

## 7. Secret và cấu hình local

1. Copy `.env.example` thành `.env`.
2. Sinh JWT secret Base64 tối thiểu 32 byte, ví dụ PowerShell:

```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

3. Tạo `RESERVATION_SERVICE_CLIENT_SECRET` dài, ngẫu nhiên.
4. Không commit `.env`. `.gitignore` đã bỏ qua file này.
5. Trước mỗi terminal Java, dot-source script:

```powershell
. .\scripts\Import-LocalEnv.ps1
```

## 8. Giới hạn/future work

Đây là local training implementation: HMAC shared signing secret và client secret cần được quản lý bằng secret manager ở production. Nâng cấp phù hợp là OAuth 2.0/OIDC, asymmetric key/JWKS, key rotation, refresh tokens, rate limiting, TLS/mTLS và audit persistence. Không log password hoặc full JWT.
