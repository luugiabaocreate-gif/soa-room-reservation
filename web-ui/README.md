# Campus Space Web UI

Campus Space là browser client của các capability SOA trong project, không phải
một CRUD screen nối trực tiếp vào database.

```text
Login Portal (/) → API Gateway → Auth Service
Operations Application (/app.html) → API Gateway → Room / Reservation Service
Reservation Service → Eureka discovery + machine token → Room / Auth Service
Reservation Service → RabbitMQ → Audit Service
All business services + API Gateway → Service Registry (Eureka)
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

1. Service Registry ở `http://localhost:8761`.
2. API Gateway ở `http://localhost:8088`.
3. Auth, Room, Reservation và Audit Service đã đăng ký `UP` trên Eureka.
4. RabbitMQ được chạy nếu cần quan sát lifecycle event.
5. Node.js 18+.

Browser chỉ gọi API Gateway. Reservation Service tìm Room/Auth bằng Service ID qua Eureka; browser không gọi từng service port trực tiếp.

## Chạy UI

Cách chạy toàn bộ dự án được khuyến nghị (Docker backend + Gateway + Vite UI) là từ root repository:

```powershell
.\Start-Project.cmd
```

Launcher chỉ báo `PROJECT READY` sau khi Eureka có đủ service cần thiết và UI đã trả HTTP thành công tại `http://localhost:5173`.

Chỉ khi debug riêng UI mới chạy thủ công:

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

Biến chính:

```text
VITE_API_GATEWAY_BASE_URL
VITE_SERVICE_REGISTRY_URL
```

Các direct-URL override chỉ dành cho diagnostic cô lập; không dùng chúng cho browser flow bình thường. Không commit `.env` và không đưa service secret vào browser.

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
