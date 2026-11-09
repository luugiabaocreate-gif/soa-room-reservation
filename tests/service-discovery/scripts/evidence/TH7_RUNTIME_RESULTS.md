# TH7 Runtime Evidence

## TH7-01 — Eureka registration

Đạt. Eureka Dashboard hiển thị:

- API-GATEWAY UP (1)
- AUTH-SERVICE UP (1)
- AUDIT-SERVICE UP (1)
- RESERVATION-SERVICE UP (1)
- ROOM-SERVICE UP (2): room-1, room-2

## TH7-02 / TH7-03 — Consumer-driven distribution

Đạt. 20 request đi theo Gateway → Reservation → Eureka/LoadBalancer → ROOM-SERVICE.

- room-1:8080: 10 request
- room-2:8084: 10 request

## TH7-04 — One provider instance down

Đạt. Khi một ROOM-SERVICE instance dừng, 20 request vẫn thành công qua instance còn lại.

## TH7-05 — All Room provider instances down

Đạt. Khi cả hai ROOM-SERVICE instance dừng:

- HTTP 503 Service Unavailable
- code: ROOM_SERVICE_UNAVAILABLE
- Consumer không treo vô hạn.

## TH7-06 — Re-registration / recovery

Đạt. Sau khi khởi động lại Room instances, Eureka hiển thị lại ROOM-SERVICE UP (2): room-1 và room-2.
