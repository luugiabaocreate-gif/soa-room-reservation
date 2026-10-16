# TH3 — API nâng cao và Web UI

## Versioning decision

TH1/TH2 đã có Postman Collection gọi `GET /api/v1/rooms` và `GET /api/v1/reservations`, đồng thời kiểm tra response là raw JSON array. Đổi các endpoint v1 này thành page object sẽ làm **hỏng client đã có**.

Vì vậy nhóm triển khai versioning thật:

| Contract | Mục đích |
|---|---|
| `/api/v1/rooms` và `/api/v1/reservations` | Contract TH1/TH2 giữ tương thích ngược: raw list. |
| `/api/v2/rooms` và `/api/v2/reservations` | Contract TH3: query parameter + page metadata. Web UI dùng v2 để list/filter. |

POST tạo Reservation và GET Room theo id vẫn dùng v1 vì JSON contract đó không có thay đổi breaking.

## Room query API v2

```text
GET /api/v2/rooms
  ?keyword=A
  &status=AVAILABLE
  &type=LAB
  &minCapacity=30
  &page=0
  &size=5
  &sort=capacity,desc
```

- `keyword` tìm không phân biệt hoa/thường trong `code` hoặc `building`.
- `size` từ 1 đến 50; `page >= 0`.
- `sort` là một trong: `id`, `code`, `building`, `floor`, `capacity`, `type`, `status`, kèm `asc` hoặc `desc`.
- Không có kết quả trả 200 và `content: []`.
- Endpoint `/api/v1/rooms/search` vẫn tồn tại để tương thích Collection TH1.

Ví dụ response:

```json
{
  "content": [],
  "page": 0,
  "size": 5,
  "totalElements": 0,
  "totalPages": 0,
  "sort": "capacity,desc"
}
```

## Reservation query API v2

```text
GET /api/v2/reservations
  ?roomId=1
  &requesterId=SV001
  &status=CONFIRMED
  &from=2026-10-18T08:00:00
  &to=2026-10-18T17:00:00
  &page=0
  &size=5
  &sort=startTime,asc
```

- `from/to` lọc những Reservation giao với khoảng thời gian được yêu cầu.
- `from` không được sau `to`.
- Reservation Service vẫn lấy Room qua REST. Query API không có quyền đọc Room database.

## Error response chung

Mọi lỗi API mới có tối thiểu:

```json
{
  "timestamp": "2026-10-16T17:00:00Z",
  "status": 400,
  "code": "BAD_REQUEST",
  "message": "...",
  "path": "/api/v2/rooms"
}
```

Với Bean Validation, field errors nằm thêm trong `errors`.

## API documentation

Sau khi chạy từng service:

| Service | OpenAPI JSON | Swagger UI |
|---|---|---|
| Room | `http://localhost:8080/v3/api-docs` | `http://localhost:8080/swagger-ui.html` |
| Reservation | `http://localhost:8081/v3/api-docs` | `http://localhost:8081/swagger-ui.html` |

Swagger UI hiển thị đồng thời nhóm endpoint v1 và v2.
