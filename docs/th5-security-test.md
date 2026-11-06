# TH5 — Security regression checklist

## Runtime baseline

- Docker `rabbitmq`, `room-db`, `reservation-db`, `auth-db` healthy.
- Auth `8083`, Room `8080`, Reservation `8081`, Audit `8082` running.
- Không hiển thị `.env`, JWT secret, service secret, full JWT hoặc password hash.

## Test artifacts

```text
postman/TH5_Security.postman_collection.json
    Collection gốc 01–09, giữ nguyên để tái hiện KT1.

postman/TH5_Security_Extended_Evidence.postman_collection.json
    Regression mở rộng: thêm Manager create Room 201, Reservation time động,
    Student02 isolation.

security-design/test-results.md
    Báo cáo runtime S01–S13 đã đạt.
```

## Chạy theo thứ tự

1. Login Student01 → lưu `student01Token`.
2. Login sai → 401 `INVALID_CREDENTIALS`.
3. Room không token → 401 `AUTHENTICATION_REQUIRED`.
4. Student01 đọc Room → 200.
5. Student01 POST Room → 403 `ACCESS_DENIED`.
6. Login Room Manager → POST Room code unique → 201.
7. Student01 create Reservation với body `requesterId=STUDENT-002` → 201 nhưng response owner là `STUDENT-001`.
8. Student01 query `requesterId=STUDENT-002` → 200 nhưng content chỉ Student01 hoặc rỗng.
9. Login Student02 → query Reservation → 200 nhưng không có record Student01.
10. Sau Reservation 201, kiểm Audit terminal nhận `reservation.confirmed`.
11. Chỉ sau khi hiểu từng response mới chạy Collection Runner để regression lặp lại.

## HTTP semantics cần xác nhận

```text
200 OK       Request được xử lý thành công trong phạm vi quyền hiện có.
201 Created  Server đã tạo resource mới.
401          Caller chưa được xác thực: thiếu/sai/hết hạn JWT hoặc credential sai.
403          JWT hợp lệ nhưng role/policy không cho action.
404          Route/resource không tồn tại; kiểm endpoint hoặc room id.
409          Conflict nghiệp vụ; đổi Room code hoặc time slot.
```

## Evidence cần giữ

- Video source → variables/Base URL → từng request/response → Audit terminal → Runner.
- `security-design/test-results.md` với kết luận **Đạt** từng mục.
- Không bắt buộc screenshot; terminal log/video là evidence chính.
