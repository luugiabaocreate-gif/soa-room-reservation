# TH7 — Runbook và protocol chứng minh Service Discovery

Tài liệu này là hướng dẫn vận hành, không khẳng định sẵn kết quả runtime. Chỉ đánh dấu **Đạt** sau khi chạy đúng trên môi trường local/Docker của nhóm.

## Điều kiện

- Java 17+, Docker Desktop nếu chạy Compose, Node 18+ nếu chạy Web UI.
- `.env` local chứa `JWT_SECRET` và `RESERVATION_SERVICE_CLIENT_SECRET`; không quay video hoặc commit secret.
- Không cần sửa source để chạy instance Room thứ hai: chỉ truyền environment variable khác.

## Phương án A — Docker Compose (khuyến nghị)

Từ thư mục root:

```powershell
docker compose -f docker-compose.yml -f docker-compose.th7.yml up --build
```

Điểm truy cập:

| Thành phần | URL |
|---|---|
| Eureka Dashboard | `http://localhost:8761` |
| API Gateway | `http://localhost:8088` |
| Auth API qua Gateway | `http://localhost:8088/api/v1/auth/login` |
| Room instance 1 | `http://localhost:8080` |
| Room instance 2 | `http://localhost:8084` |
| Reservation API | `http://localhost:8081` |

## Phương án B — Chạy process local

Mở các PowerShell riêng. Mỗi shell load local environment trước khi chạy service cần secret/database:

```powershell
. .\scripts\Import-LocalEnv.ps1
```

Khởi động hạ tầng trước:

```powershell
docker compose up -d
```

Sau đó khởi động theo thứ tự:

```powershell
Set-Location .\service-registry
.\mvnw.cmd spring-boot:run
```

```powershell
Set-Location .\auth-service
. ..\scripts\Import-LocalEnv.ps1
.\mvnw.cmd spring-boot:run
```

```powershell
Set-Location .\room-service
. ..\scripts\Import-LocalEnv.ps1
$env:SERVER_PORT="8080"
$env:INSTANCE_ID="room-1"
.\mvnw.cmd spring-boot:run
```

Mở PowerShell khác cho Room instance 2:

```powershell
Set-Location .\room-service
. ..\scripts\Import-LocalEnv.ps1
$env:SERVER_PORT="8084"
$env:INSTANCE_ID="room-2"
.\mvnw.cmd spring-boot:run
```

```powershell
Set-Location .\reservation-service
. ..\scripts\Import-LocalEnv.ps1
.\mvnw.cmd spring-boot:run
```

```powershell
Set-Location .\audit-service
.\mvnw.cmd spring-boot:run
```

```powershell
Set-Location .\api-gateway
.\mvnw.cmd spring-boot:run
```

Cuối cùng chạy Web UI:

```powershell
Set-Location .\web-ui
npm install
npm run dev
```

## Protocol kiểm chứng TH7

Dùng file `tests/service-discovery/postman/TH7_Service_Registry.postman_collection.json`. Login qua Gateway trước để collection lưu token. Không dùng Runner trong video; gọi request discovery từng lần.

Để tạo evidence lặp lại được cho mốc 20 request, có thể dùng script (đăng nhập qua Gateway và gọi chính discovery probe qua Consumer):

```powershell
.\tests\service-discovery\scripts\Invoke-TH7DiscoveryEvidence.ps1 -RequestCount 20
```

Script ghi CSV + JSON theo thời gian vào `tests/service-discovery/scripts/evidence/` và báo lỗi nếu một call thất bại hoặc chưa quan sát đủ hai Room provider. Chỉ lưu/đính kèm output sau khi kiểm tra cả hai instance đã `UP` trong Eureka.

| Mã | Thao tác | Kết quả cần quan sát | Trạng thái runtime |
|---|---|---|---|
| TH7-01 | Mở Eureka Dashboard | Có `AUTH-SERVICE`, hai `ROOM-SERVICE`, `RESERVATION-SERVICE`, `AUDIT-SERVICE`, `API-GATEWAY` | Chưa thực thi trong source package |
| TH7-02 | Login qua Gateway | `200`, token được cấp bởi Auth Service sau Gateway route | Chưa thực thi trong source package |
| TH7-03 | Gọi discovery probe qua Gateway 20 lần, từng request | `200`; `instanceId`/`port` có cả `room-1:8080` và `room-2:8084` theo thời gian | Chưa thực thi trong source package |
| TH7-04 | Dừng Room instance 2, tiếp tục probe sau khi Registry cập nhật | `200`; chỉ instance còn lại phục vụ | Chưa thực thi trong source package |
| TH7-05 | Dừng cả hai Room instance, gọi probe hoặc flow Reservation | Lỗi controlled; không treo vô hạn, không tạo Reservation | Chưa thực thi trong source package |
| TH7-06 | Khởi động lại một Room instance | Instance đăng ký lại và được Consumer khám phá | Chưa thực thi trong source package |

Khi có runtime evidence local, thay trạng thái bằng HTTP result/log thực tế, không ghi kết quả suy đoán.

## Deploy không phụ thuộc localhost

Ở môi trường chung, không để browser build với `localhost`. Khi build Web UI, cung cấp URL Gateway thật:

```powershell
$env:VITE_API_GATEWAY_BASE_URL="https://api.example.edu"
npm run build
```

Ở Gateway/downstream, set `WEB_UI_ORIGIN` thành domain Web UI thật. Service-to-service dùng Eureka trong network nội bộ thông qua `EUREKA_DEFAULT_ZONE`; không dùng browser hostname/port cho discovery.
