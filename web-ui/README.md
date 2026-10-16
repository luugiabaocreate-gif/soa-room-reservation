# Room Reservation Web UI

Đây là browser client của Room Service và Reservation Service. UI chỉ gọi REST API; không gọi RabbitMQ và không truy cập database trực tiếp.

## 1. Điều kiện chạy

1. Room Service đang chạy ở `http://localhost:8080`.
2. Reservation Service đang chạy ở `http://localhost:8081`.
3. Hai service đã áp dụng cấu hình CORS `http://localhost:5173`.
4. Node.js 18+ được khuyến nghị cho Vite 6.

## 2. Chạy UI

```powershell
Set-Location D:\soa-room-reservation\web-ui
npm install
npm run dev
```

Mở URL Vite hiển thị trong terminal, thông thường là `http://localhost:5173`.

## 3. Cấu hình URL API (khi cần)

Mặc định UI gọi:

```text
Room API v1 (GET by id):         http://localhost:8080/api/v1
Reservation API v1 (create):      http://localhost:8081/api/v1
Room query API v2 (list/filter):  http://localhost:8080/api/v2
Reservation query API v2 (list):  http://localhost:8081/api/v2
```

Nếu môi trường của nhóm dùng URL khác, copy `.env.example` thành `.env`, rồi sửa giá trị. Không commit `.env`.

```powershell
Copy-Item .env.example .env
```

## 4. Kiểm thử bắt buộc

1. Room Explorer: lọc `status=AVAILABLE`, loại `LAB`, `minCapacity=40`, sort `capacity,desc`.
2. Chọn một phòng `AVAILABLE`, gửi Reservation hợp lệ, UI phải hiển thị `CONFIRMED`.
3. Gửi lại cùng Room với thời gian overlap, UI phải hiển thị HTTP `409`.
4. Nhập Room id không tồn tại, UI phải hiển thị HTTP `404`.
5. Tắt Room Service, gửi Reservation mới, UI phải hiển thị HTTP `503`.
6. Kiểm tra browser Console: không có CORS error.

> UI chỉ biết Reservation được confirm qua HTTP 201. Việc Audit Service consume RabbitMQ event là bất đồng bộ; UI không được khẳng định event audit đã hoàn tất.
