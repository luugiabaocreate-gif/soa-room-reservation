# TH6 — Thiết kế tích hợp Reservation ↔ Room

## 1. Quyết định phạm vi

TH6 không dựng một hệ thống mới. Nhóm tái sử dụng các service đã có để hoàn thành một nghiệp vụ xuyên service thực tế:

```text
Student tạo Reservation
  → Reservation Service xác thực user JWT và ownership
  → Reservation Service xin machine token từ Auth Service
  → Reservation Service gọi Room Service để xác minh Room
  → Reservation Service kiểm tra capacity / lịch trùng / lưu reservation_db
  → Reservation Service publish reservation.confirmed
  → Audit Service nhận event
```

### Consumer và Provider

```text
Consumer chính: Reservation Service
Provider chính: Room Service
Provider hỗ trợ: Auth Service (machine token)
Consumer event: Audit Service
```

Reservation Service không đọc `room_db` trực tiếp. Room Service vẫn sở hữu trạng thái/capacity Room; Reservation chỉ lưu `roomId` và `roomCode` như external reference.

## 2. TH6 khác TH2 ở đâu?

TH2 đã giới thiệu lời gọi REST đồng bộ và RabbitMQ. TH6 đóng gói thành một nghiệp vụ hoàn chỉnh, có contract và kết quả kiểm tra rõ:

- Consumer dùng `RestClient` thực sự trong `RoomClient`, không phải Postman gọi hộ.
- Caller identity được tách: user JWT cho Browser → Reservation, machine JWT cho Reservation → Room.
- Response từ Provider được chuyển thành `RoomResponse`, sau đó kiểm trạng thái Room và capacity.
- Consumer lưu liên kết `roomId`/`roomCode` trong database của chính Reservation Service.
- Consumer xử lý Room không tồn tại, Room không sẵn sàng, conflict lịch và Room Service unavailable.
- Event chỉ publish sau local persistence; Audit nhận event độc lập.

## 3. Source trace

```text
reservation-service/.../reservation/ReservationController.java
  POST /api/v1/reservations
      ↓
reservation-service/.../reservation/ReservationService.java
  enforceCreateOwnership
  validateTimeRange
  roomClient.getRoomById
  validateRoomAvailability
  validateCapacity
  reservationPersistenceService.createConfirmedReservation
  reservationEventPublisher.publishReservationConfirmed
      ↓
reservation-service/.../client/ServiceTokenClient.java
  POST Auth /api/v1/auth/service-token
      ↓
reservation-service/.../client/RoomClient.java
  GET Room /api/v1/rooms/{id} + Bearer machine JWT
      ↓
room-service/.../room/RoomController.java
  GET /api/v1/rooms/{id}
```

## 4. Business rule

Một Reservation chỉ `CONFIRMED` khi toàn bộ điều kiện sau pass:

1. User JWT hợp lệ và role được phép tạo Reservation.
2. Student không thể giả `requesterId`/`requesterName` trong body.
3. Room tồn tại, `status=AVAILABLE` và capacity đáp ứng attendee count.
4. Không có Reservation `CONFIRMED` trùng Room/time slot.
5. Reservation được local transaction commit vào `reservation_db`.

Audit event là side effect sau commit. Nếu RabbitMQ không nhận đúng lúc publish, Reservation đã `CONFIRMED` không bị rollback; đây là giới hạn hiện tại, hướng nâng cấp là transactional outbox.

## 5. Lỗi tích hợp được xử lý

```text
Room id không tồn tại              → 404 ROOM_NOT_FOUND
Room MAINTENANCE/LOCKED/...         → 409 ROOM_NOT_AVAILABLE
Attendee vượt capacity              → 409 ROOM_NOT_AVAILABLE
Trùng khung giờ Room                → 409 ROOM_SCHEDULE_CONFLICT
Room Service timeout/unavailable    → retry rồi 503 ROOM_SERVICE_UNAVAILABLE
Không/sai Bearer JWT                → 401 AUTHENTICATION_REQUIRED
JWT hợp lệ nhưng role thiếu         → 403 ACCESS_DENIED
```

## 6. Tính nhất quán và retry

`ReservationPersistenceService` chỉ transaction trong `reservation_db`; không có distributed transaction với Room Service hay RabbitMQ.

- REST Room lookup chạy trước local persistence.
- `RoomClient` retry khi lỗi network/5xx theo cấu hình `room.service.retry.*`.
- Nếu Provider Room không phản hồi sau retry, Consumer trả `503`; không persist Reservation mới.
- Nếu local persistence thành công nhưng publish Audit event lỗi, Reservation vẫn confirmed; log ghi event gap để vận hành nhận biết.

## 7. Artifacts TH6

```text
integration/
├── integration-design.md
├── api-contract.md
├── sequence-diagram/reservation-room-confirmation.mmd
├── postman/reservation-room-integration-tests.json
└── test-results.md
```
