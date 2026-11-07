# Campus Space Web UI

Campus Space là browser client của các capability SOA trong project, không phải
một CRUD screen nối trực tiếp vào database.

```text
Login Portal (/) → Auth Service
Operations Application (/app.html) → Room Service + Reservation Service
Reservation Service → Auth machine token → Room Service
Reservation Service → RabbitMQ → Audit Service
```

Browser **không** truy cập database/RabbitMQ, không chứa machine credential và
không quyết định authorization thay backend.

## Hai giao diện độc lập

| URL | Mục đích |
|---|---|
| `/` | **Login Portal**. Chỉ xử lý username/password và nhận user JWT từ Auth Service. |
| `/app.html` | **Operations Application**. Chỉ mở khi session JWT hợp lệ. Có dashboard và workspace theo role. |

Token chỉ nằm trong `sessionStorage`, không lưu password. Khi JWT hết hạn UI
đưa người dùng về Login Portal; API vẫn tự verify token ở server.

## Workspace theo role

| Role | Giao diện và capability |
|---|---|
| `STUDENT` | Tìm room theo thời gian/số người, tạo Reservation của chính mình, xem/hủy Reservation của chính mình. |
| `ROOM_MANAGER` | Dashboard điều phối, xem Reservation theo policy, tạo/sửa/xóa/chuyển state Room. Không có capability tạo Reservation. |
| `ADMIN` | Room Operations và Reservation operations theo authority ở backend. |

Việc UI ẩn một menu không phải authorization. `SecurityConfig` của Room Service
và Reservation Service mới là nơi quyết định cuối cùng.

## Availability API

Booking wizard gọi chính endpoint mới thuộc **Reservation Service**:

```http
GET /api/v3/availability?from=...&to=...&attendeeCount=...&type=LAB
Authorization: Bearer <user JWT>
```

Reservation Service dùng machine JWT để query Room Provider, sau đó so lịch
`CONFIRMED` trong `reservation_db` của nó. Response chỉ có trạng thái candidate
`AVAILABLE` hoặc `SCHEDULE_CONFLICT`; không lộ người đặt/mục đích của người
khác. Lúc tạo Reservation, service vẫn kiểm tra lại nên UI xử lý 409 như một
business result hợp lệ.

## Điều kiện chạy

1. Auth Service ở `http://localhost:8083`.
2. Room Service ở `http://localhost:8080`.
3. Reservation Service ở `http://localhost:8081` (có Availability API v3 và cancellation API).
4. RabbitMQ/Audit Service được chạy nếu cần quan sát lifecycle event.
5. Node.js 18+.

## Chạy UI

```powershell
Set-Location D:\soa-room-reservation\web-ui
npm install
npm run dev
```

Mở URL Vite, thông thường là `http://localhost:5173`. Vite build được cấu hình
multi-page, nên `index.html` và `app.html` đều được build độc lập.

## Cấu hình URL API

Copy file mẫu khi các service không chạy ở localhost mặc định:

```powershell
Copy-Item .env.example .env
```

Các biến:

```text
VITE_AUTH_API_BASE_URL
VITE_ROOM_API_BASE_URL
VITE_ROOM_QUERY_API_BASE_URL
VITE_RESERVATION_API_BASE_URL
VITE_RESERVATION_QUERY_API_BASE_URL
VITE_AVAILABILITY_API_BASE_URL
```

Không commit `.env` và không đưa service secret vào browser.

## Smoke test Campus Operations

1. Vào `/`, login `student01`; phải redirect sang `/app.html`.
2. Mở **Tìm & đặt phòng**, chọn future start/end, số người và loại Room.
3. Availability API phải trả candidate từ Room Provider và đánh dấu lịch trùng
   mà không hiện identity/purpose của Reservation khác.
4. Chọn `AVAILABLE`, nhập mục đích, submit; kết quả là `201 CONFIRMED` hoặc
   controlled `409` nếu race conflict.
5. Mở **Đăng ký của tôi**, hủy một `CONFIRMED`; kết quả là `CANCELLED` và event
   `reservation.cancelled` được publish.
6. Login `roommanager01`; menu **Vận hành phòng** hiện, menu booking không hiện.
   Tạo/sửa Room qua Room Service API, không qua database.
7. Login `student01` lại: URL Room mutation nếu gọi trực tiếp vẫn phải nhận 403
   từ backend, bất kể menu đã bị ẩn.
