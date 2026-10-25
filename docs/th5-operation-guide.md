# TH5 — Hướng dẫn thao tác: Auth Service, JWT, Role và Ownership

Tài liệu này bổ sung cho hướng dẫn chạy TH1–TH4. Nó áp dụng cho source TH5 ở branch `feature/th5-auth-security` và hướng dẫn thao tác theo đúng thứ tự: cấu hình secret local → Docker → Auth → Room → Reservation → Audit → Web UI → kiểm thử bảo mật.

## 1. TH5 bổ sung gì và vì sao phải chạy theo thứ tự mới?

| Thành phần | Port | Trách nhiệm TH5 |
|---|---:|---|
| Auth Service | 8083 | Owns `auth_db`; kiểm tra BCrypt password và cấp JWT 900 giây. |
| Room Service | 8080 | Xác minh JWT; chỉ `ROOM_MANAGER`/`ADMIN` được sửa Room. |
| Reservation Service | 8081 | Xác minh JWT, role và ownership; Student chỉ có dữ liệu của mình. |
| Audit Service | 8082 | Nhận event RabbitMQ, không cần JWT browser. |
| Web UI | 5173 | Đăng nhập, giữ JWT trong `sessionStorage`, gửi Bearer JWT. |
| RabbitMQ | 5672 / 15672 | Broker/event audit. |
| PostgreSQL | 5433 / 5434 / 5435 | `room_db`, `reservation_db`, `auth_db` tách biệt. |

```text
Browser -- username/password --> Auth Service -- JWT --> Room / Reservation API
Reservation Service -- client credential --> Auth Service -- service JWT --> Room Service
Reservation Service -- event --> RabbitMQ --> Audit Service
```

Reservation Service không chuyển tiếp token của Student sang Room Service. Nó dùng machine token riêng có audience `room-service`.

## 2. Một lần trước khi chạy: source branch và `.env`

Tại root repository:

```powershell
Set-Location D:\soa-room-reservation
git status -sb
git switch -c feature/th5-auth-security
```

> Chỉ dùng `git switch -c` nếu branch chưa tồn tại. Nếu đã tồn tại, dùng `git switch feature/th5-auth-security`.

Copy toàn bộ source TH5 batch vào root repository. Sau đó kiểm tra:

```powershell
git diff --check
git status --short
```

### 2.1 Tạo `.env`

Kiểm tra file `.env` đã có chưa:

```powershell
Test-Path -LiteralPath .env
```

Nếu kết quả `False`:

```powershell
Copy-Item -LiteralPath .env.example -Destination .env
```

Nếu đã có `.env` từ TH4, giữ các giá trị local cũ và thêm các dòng Auth/JWT còn thiếu. Không xóa `.env` nếu nó có port/credential local do nhóm tự đổi.

### 2.2 Tạo hai secret local

Chạy trong PowerShell; không gửi hai output này lên GitHub/chat:

```powershell
$jwtBytes = New-Object byte[] 32
$randomGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$randomGenerator.GetBytes($jwtBytes)
$jwtSecret = [Convert]::ToBase64String($jwtBytes)

$alphabet = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
$serviceSecret = -join (
    1..48 | ForEach-Object {
        $alphabet[(Get-Random -Maximum $alphabet.Length)]
    }
)

$jwtSecret
$serviceSecret
```

Mở file để sửa:

```powershell
notepad .env
```

File `.env` phải có tối thiểu:

```properties
AUTH_DB_NAME=auth_db
AUTH_DB_USERNAME=auth_user
AUTH_DB_PASSWORD=auth_local_dev
AUTH_DB_PORT=5435
JWT_SECRET=<paste JWT secret vừa tạo, không có dấu nháy>
RESERVATION_SERVICE_CLIENT_SECRET=<paste service secret vừa tạo, không có dấu nháy>
```

Lưu file, rồi kiểm tra `.env` không bị Git theo dõi:

```powershell
git status --short
```

Không được thấy `.env` trong output.

## 3. Khởi động Docker infrastructure

```powershell
Set-Location D:\soa-room-reservation
docker compose up -d
docker compose ps
```

Chờ tới khi bốn container đều `healthy`:

```text
soa-rabbitmq
soa-room-db
soa-reservation-db
soa-auth-db
```

Không chạy `docker compose down -v` trong quá trình test bình thường vì nó xóa Room/Reservation/Auth database local.

## 4. Chạy năm terminal theo đúng thứ tự

Mỗi Spring Boot service phải chạy ở PowerShell riêng. Terminal `spring-boot:run` phải giữ mở; không gõ lệnh khác vào terminal đó.

### Terminal 1 — Auth Service

```powershell
Set-Location D:\soa-room-reservation
. .\scripts\Import-LocalEnv.ps1
Set-Location .\auth-service
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Chờ `Database JDBC URL [jdbc:postgresql://localhost:5435/auth_db]`, `Tomcat started on port 8083`, `Started AuthServiceApplication`. Lần đầu Auth Service seed bốn demo account vào `auth_db` với BCrypt hash.

### Terminal 2 — Room Service

```powershell
Set-Location D:\soa-room-reservation
. .\scripts\Import-LocalEnv.ps1
Set-Location .\room-service
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Chờ JDBC `localhost:5433/room_db`, port 8080 và `Started RoomServiceApplication`.

### Terminal 3 — Reservation Service

```powershell
Set-Location D:\soa-room-reservation
. .\scripts\Import-LocalEnv.ps1
Set-Location .\reservation-service
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Chờ JDBC `localhost:5434/reservation_db`, port 8081, `Started ReservationServiceApplication` và RabbitMQ topology declared.

### Terminal 4 — Audit Service

```powershell
Set-Location D:\soa-room-reservation\audit-service
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Chờ port 8082, RabbitMQ connection và `Started AuditServiceApplication`.

### Terminal 5 — Web UI

```powershell
Set-Location D:\soa-room-reservation\web-ui
npm ci
npm run dev
```

Mở URL `Local:` mà Vite in ra, thường là `http://localhost:5173`.

## 5. Đăng nhập và demo happy path

| Username | Password | Expected role |
|---|---|---|
| `student01` | `DemoPassword123!` | STUDENT / `STUDENT-001` |
| `student02` | `DemoPassword123!` | STUDENT / `STUDENT-002` |
| `roommanager01` | `DemoPassword123!` | ROOM_MANAGER |
| `admin01` | `DemoPassword123!` | ADMIN |

1. Mở Web UI và login `student01`.
2. UI chỉ hiển thị app sau login; token nằm trong `sessionStorage` của tab browser.
3. Filter Room, ví dụ A301 (id 2), trạng thái AVAILABLE.
4. Tạo Reservation với thời gian tương lai, không overlap, số người không vượt capacity.
5. Response/UI phải có `CONFIRMED`.
6. Terminal Audit phải có `AUDIT EVENT RECEIVED`.

Đối với Student, server tự quyết requesterId/requesterName từ JWT; body do UI gửi không được coi là identity đáng tin cậy.

## 6. Security evidence bắt buộc

Dùng Postman collection `postman/TH5_Security.postman_collection.json` hoặc Swagger/PowerShell. Cần hoàn thành và ghi vào `security-design/test-results.md`.

| Evidence | Expected |
|---|---|
| Login đúng | 200, `accessToken`, `tokenType=Bearer`, `expiresIn=900` |
| Sai password | 401 `INVALID_CREDENTIALS`, không tiết lộ username tồn tại hay không |
| Room API không token | 401 `AUTHENTICATION_REQUIRED` |
| Room API JWT sai/hết hạn | 401 |
| Student GET Room | 200 |
| Student POST Room | 403 `ACCESS_DENIED` |
| Room manager POST/PUT Room | 201/200 hợp lệ |
| student01 body giả `STUDENT-002` khi create Reservation | 201 chỉ với requesterId `STUDENT-001` |
| student01 query requesterId `STUDENT-002` | 200, content chỉ của `STUDENT-001` |
| student02 xem list | Không thấy Reservation của student01 |
| Reservation → Room lookup | Vẫn thành công bằng machine token riêng |

Phân biệt chuẩn: `401` = không có/không xác thực được token; `403` = token đúng nhưng role không đủ; `404` = tài nguyên không tồn tại theo chính sách API; `400` = dữ liệu request sai.

## 7. Các URL kỹ thuật

```text
Web UI:              http://localhost:5173
Auth Swagger:        http://localhost:8083/swagger-ui.html
Room Swagger:        http://localhost:8080/swagger-ui.html
Reservation Swagger: http://localhost:8081/swagger-ui.html
RabbitMQ UI:         http://localhost:15672
```

RabbitMQ local: `soa` / `soa-local-dev`.

Trong Room/Reservation Swagger, bấm **Authorize**, nhập `Bearer <token>` sau khi login qua Auth Service.

## 8. Stop/restart và troubleshooting nhanh

- Dừng Java/Web UI: `Ctrl + C` từng terminal.
- Dừng Docker nhưng giữ data: `docker compose stop`.
- Chạy lại: `docker compose up -d` rồi start Auth → Room → Reservation → Audit → UI.
- `docker compose down` xóa container/network nhưng giữ named volume.
- `docker compose down -v` xóa toàn bộ local DB data.

| Hiện tượng | Kiểm tra/cách xử lý |
|---|---|
| `HikariPool-1 - Starting...` | `docker compose ps`; Docker Engine phải Running và database đúng health. |
| Auth/Room/Reservation báo `${JWT_SECRET}` hoặc secret placeholder | Chưa dot-source `. .\scripts\Import-LocalEnv.ps1`, hoặc `.env` còn `REPLACE_...`. |
| 401 sau login | Kiểm tra Auth/Room/Reservation dùng cùng một `JWT_SECRET`; restart cả ba service sau khi đổi `.env`. |
| 403 Student POST Room | Đây là behavior đúng TH5. Dùng roommanager01/admin01. |
| Reservation 503 | Kiểm tra Auth Service và Room Service đang chạy; Reservation cần Auth machine token để gọi Room. |
| Docker Desktop WSL needs updating | PowerShell Administrator: `wsl --update`, `wsl --shutdown`, mở lại Docker Desktop. |

## 9. Trước khi commit TH5

```powershell
Set-Location D:\soa-room-reservation
git diff --check
git status --short
```

Không stage `.env`, `node_modules`, `target`, Docker volumes hay password/token thực. Chỉ commit source/config template/docs/Postman/test evidence không chứa secret.
