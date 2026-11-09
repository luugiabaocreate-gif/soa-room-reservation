# TH7 — Service Registry, Discovery và API Gateway

## Mục tiêu đã áp dụng cho SOA Room Reservation

TH7 thay cách **Reservation Service định vị Room Service và Auth Service**. Trước TH7, các lời gọi nội bộ phụ thuộc `localhost` cùng một port cố định. Sau TH7, Consumer gọi bằng Service ID; Eureka giữ danh sách instance và Spring Cloud LoadBalancer chọn một instance còn đăng ký.

```text
Browser / Web UI
        │ một gateway URL
        ▼
API-GATEWAY :8088 ── lb://AUTH-SERVICE ───────► AUTH-SERVICE
        │              lb://ROOM-SERVICE ───────► ROOM-SERVICE instance 1 :8080
        │              lb://RESERVATION-SERVICE ─► RESERVATION-SERVICE :8081
        │                                           │ lb://AUTH-SERVICE
        │                                           └─ lb://ROOM-SERVICE ─► ROOM-SERVICE instance 2 :8084
        ▼
SERVICE-REGISTRY :8761 (Eureka)
        ▲
        └── AUTH-SERVICE, ROOM-SERVICE, RESERVATION-SERVICE,
            AUDIT-SERVICE và API-GATEWAY tự đăng ký / heartbeat
```

`SERVICE-REGISTRY` không xử lý nghiệp vụ, không sở hữu database và không thay thế API Gateway. API Gateway là điểm vào browser; Eureka là danh bạ nội bộ cho các process.

## Service ownership và Service ID

| Service ID | Module | Trách nhiệm | Dữ liệu / hạ tầng sở hữu | Consumer chính |
|---|---|---|---|---|
| `AUTH-SERVICE` | `auth-service` | Login, BCrypt, phát user/service JWT | `auth_db` | Web UI, Reservation Service |
| `ROOM-SERVICE` | `room-service` | Room catalog và trạng thái hoạt động | `room_db` | Web UI qua Gateway, Reservation Service |
| `RESERVATION-SERVICE` | `reservation-service` | Reservation, ownership, availability, cancellation | `reservation_db` | Web UI qua Gateway |
| `AUDIT-SERVICE` | `audit-service` | Consume lifecycle event | RabbitMQ queue, không sở hữu business DB | RabbitMQ |
| `API-GATEWAY` | `api-gateway` | Route browser HTTP đến Service ID | Không sở hữu business data | Web UI |
| `SERVICE-REGISTRY` | `service-registry` | Registration / discovery instance | Registry state, không phải business DB | Tất cả process |

## Chuyển đổi Consumer bắt buộc

### Trước TH7

`reservation-service/src/main/resources/application.properties` đặt địa chỉ vật lý:

```text
room.service.base-url=http://localhost:8080
auth.service.base-url=http://localhost:8083
```

### Sau TH7

Cùng file đó mặc định dùng logical Service ID:

```text
room.service.base-url=http://ROOM-SERVICE
auth.service.base-url=http://AUTH-SERVICE
```

`reservation-service/src/main/java/vn/edu/reservationservice/config/RestClientConfig.java` tạo hai `RestClient.Builder` có `@LoadBalanced`.

`reservation-service/src/main/java/vn/edu/reservationservice/client/RoomClient.java` và `ServiceTokenClient.java` vẫn giữ machine JWT, timeout, retry và mapping lỗi TH5/TH6. Khác biệt duy nhất ở tầng định vị: `http://ROOM-SERVICE` và `http://AUTH-SERVICE` được LoadBalancer đổi thành địa chỉ của một instance Eureka đang biết.

Không user JWT nào bị forward từ browser sang Room Service. Discovery không thay thế JWT, RBAC hoặc ownership.

## Hai Room instance và chứng minh LoadBalancer

`ROOM-SERVICE` chạy hai process có cùng `spring.application.name=ROOM-SERVICE`, cùng database `room_db`, nhưng khác `SERVER_PORT` / `INSTANCE_ID`:

| Instance | `SERVER_PORT` | `INSTANCE_ID` | Eureka Service ID |
|---|---:|---|---|
| Room 1 | `8080` | `room-1` | `ROOM-SERVICE` |
| Room 2 | `8084` | `room-2` | `ROOM-SERVICE` |

Cùng database là điều bắt buộc với dịch vụ có business state: hai instance không được dùng hai `room_db` độc lập để giả lập scale-out.

Room Service bổ sung metadata không nhạy cảm tại endpoint đã có `GET /api/v1/rooms/info`: `serviceId`, `instanceId`, `port`, đồng thời có `GET /api/v1/rooms/provider-info` được xác thực cho machine-to-machine lookup. Reservation Service bổ sung `GET /api/v1/discovery/room-provider`; endpoint này thực sự gọi `provider-info` qua logical Service ID với machine JWT và chỉ trả identity của instance đã được chọn. Nó là bằng chứng vận hành TH7, không phải endpoint nghiệp vụ và không lộ Room/Reservation data.

## API Gateway và browser deployment

`api-gateway/src/main/resources/application.yml` khai báo route `lb://` theo path contract hiện có:

| Browser path | Target qua Gateway |
|---|---|
| `/api/v1/auth/**` | `AUTH-SERVICE` |
| `/api/v1/rooms/**`, `/api/v2/rooms/**` | `ROOM-SERVICE` |
| `/api/v1/reservations/**`, `/api/v2/reservations/**`, `/api/v3/availability/**`, `/api/v1/discovery/**` | `RESERVATION-SERVICE` |
| `/api/v1/audits/**` | `AUDIT-SERVICE` |

Gateway không tự cấp quyền thay Service. Header `Authorization: Bearer ...` được route đến downstream; Auth/Room/Reservation vẫn tự verify chữ ký JWT, audience, role và ownership như TH5.

`web-ui/src/core/api.js` dùng `VITE_API_GATEWAY_BASE_URL` làm default gateway entry point. Local development mặc định là `http://localhost:8088`; môi trường deploy phải set biến này thành URL Gateway thật, ví dụ `https://api.example.edu`. Browser không cần và không được biết port của từng service.

## Giới hạn trung thực

- Một Eureka Server là đủ cho TH7; chưa có Eureka HA/peer replication.
- Registry/heartbeat có độ trễ. Khi một process chết không graceful, instance có thể còn hiện một khoảng thời gian trước eviction.
- Retry của Reservation khi Room Service lỗi vẫn là retry TH6; Eureka không biến lỗi downstream thành thành công.
- Transactional outbox vẫn là future work; TH7 không thay đổi semantics event RabbitMQ.
