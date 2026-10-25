# TH5 — Security test checklist

## Chuẩn bị

- Docker `rabbitmq`, `room-db`, `reservation-db`, `auth-db` healthy.
- Auth Service port `8083`, Room `8080`, Reservation `8081`, Audit `8082`, Web UI `5173` đang chạy.
- Dùng Postman hoặc PowerShell. Lưu JWT vào biến `student01Token`, `student02Token`, `managerToken`, `adminToken`.

## Login request

```http
POST /api/v1/auth/login
Content-Type: application/json

{"username":"student01","password":"DemoPassword123!"}
```

Kỳ vọng HTTP 200, `accessToken`, `tokenType=Bearer`, `expiresIn=900`.

## Required evidence

| # | Request | Account/token | Expected |
|---:|---|---|---|
| 1 | POST `/api/v1/auth/login` đúng password | student01 | 200, JWT |
| 2 | POST login sai password | student01 | 401 `INVALID_CREDENTIALS`; không tiết lộ user có tồn tại |
| 3 | GET `/api/v2/rooms` không token | none | 401 |
| 4 | GET `/api/v2/rooms` với JWT sai | invalid | 401 |
| 5 | GET `/api/v2/rooms` | student01 | 200 |
| 6 | POST `/api/v1/rooms` | student01 | 403 |
| 7 | POST/PUT Room | roommanager01 hoặc admin01 | 201/200 hợp lệ |
| 8 | POST Reservation, body cố đặt requesterId `STUDENT-002` | student01 | 201 nhưng row/response phải có requesterId `STUDENT-001` |
| 9 | GET `/api/v2/reservations?requesterId=STUDENT-002` | student01 | 200 nhưng chỉ content của `STUDENT-001` |
| 10 | Cùng request #9 | roommanager01/admin01 | 200
| 11 | POST Reservation with `requesterId=STUDENT-002` | student01 | Response/DB must be `STUDENT-001`; browser value is not trusted |
| 12 | GET Reservation v1/v2 | student02 | Must not show `STUDENT-001` data |
| 13 | POST login with disabled/nonexistent account | unknown | 401 generic `INVALID_CREDENTIALS` |
| 14 | Create Reservation | student01 | Room lookup must still work; Reservation Service uses machine token, not forwarded user token |

## Screenshot/log to preserve

1. Login success and response excludes password/hash.
2. 401 no token; 401 malformed token; 403 incorrect role.
3. Student01 denied/filtered from Student02 resource.
4. Room manager allowed to mutate Room (use a harmless test Room or update/revert).
5. `AUDIT EVENT RECEIVED` after authenticated Reservation creation.
6. Explain service-token path shown in `docs/th5-security-design.md`.
