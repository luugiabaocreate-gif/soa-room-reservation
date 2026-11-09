# One-command Local Run

## Mục tiêu

Khởi động cả SOA Room Reservation bằng một lệnh, không cần mở terminal riêng cho Docker, Auth, Room, Reservation, Audit và Web UI.

```powershell
.\Start-Project.cmd
```

## Launcher làm gì?

`Start-Project.cmd` gọi `scripts/Start-Project.ps1` với PowerShell execution policy chỉ áp dụng cho process hiện tại. Nó:

1. xác minh Docker Engine đang chạy;
2. load/validate root `.env` qua `Import-LocalEnv.ps1`;
3. chạy `docker compose -f docker-compose.yml -f docker-compose.th7.yml up -d --build`;
4. kiểm tra health HTTP của Eureka, Gateway, Auth, Room 1, Room 2, Reservation và Audit;
5. kiểm tra Eureka có đúng 5 Service ID và ít nhất 2 instance `ROOM-SERVICE`;
6. nếu Web UI chưa chạy, chạy `npm ci` khi cần và khởi động Vite ở `http://localhost:5173` trong background;
7. chỉ in `PROJECT READY` khi toàn bộ điều kiện trên đạt.

Log của Vite và PID do launcher quản lý nằm ở `.runtime/`, thư mục này bị Git ignore.

## Tham số

```powershell
# Bỏ build Docker để start nhanh khi code backend không đổi.
.\Start-Project.cmd -SkipBuild

# Chỉ chạy infrastructure/backend, không chạy Vite.
.\Start-Project.cmd -NoWebUi

# Tăng timeout startup (mặc định 300 giây).
.\Start-Project.cmd -TimeoutSeconds 600
```

## Dừng toàn bộ stack

```powershell
.\Stop-Project.cmd
```

Lệnh này dừng process tree Vite do launcher tạo, rồi thực hiện:

```text
docker compose -f docker-compose.yml -f docker-compose.th7.yml down --remove-orphans
```

Nó không dùng `-v`, vì vậy volumes PostgreSQL/RabbitMQ được giữ lại.

## Khi launcher báo lỗi

| Thông báo | Cách xử lý |
|---|---|
| Docker Engine unavailable | Mở Docker Desktop và chờ `Engine running`. |
| `.env`/secret placeholder | Tạo `.env` từ `.env.example`, thay `JWT_SECRET` và `RESERVATION_SERVICE_CLIENT_SECRET`. |
| Một service không ready | Xem `docker compose -f docker-compose.yml -f docker-compose.th7.yml logs <service> --tail 120`. |
| Eureka registration timeout | Mở `http://localhost:8761`, kiểm tra các service `UP`; xem logs `reservation-service`, `room-service-1`, `room-service-2`. |
| Web UI không lên | Xem `.runtime/logs/web-ui.out.log` và `.runtime/logs/web-ui.err.log`; kiểm tra Node.js 18+ và port 5173. |

Chạy native bằng `mvnw.cmd spring-boot:run` vẫn hợp lệ cho chẩn đoán một service, nhưng không phải quy trình thường ngày.
