# ROLE – PERMISSION MATRIX
## SOA Room Reservation — TH5

## 1. Tài khoản demo

| Account | Role | requesterId | Dùng để kiểm thử |
|---|---|---|---|
| `student01` | `STUDENT` | `STUDENT-001` | Read Room, 403 Room write, ownership Student01. |
| `student02` | `STUDENT` | `STUDENT-002` | Isolation với dữ liệu Student01. |
| `roommanager01` | `ROOM_MANAGER` | — | Tạo/sửa/xóa Room. |
| `admin01` | `ADMIN` | — | Quyền nghiệp vụ rộng. |
| `reservation-service` | `SERVICE_RESERVATION` | — | Machine identity gọi Room Service. |

Password demo local được seed bằng BCrypt; database lưu `password_hash`, không lưu password plain text.

## 2. Ma trận endpoint

| Endpoint / action | STUDENT | ROOM_MANAGER | ADMIN | SERVICE_RESERVATION | Kết quả đã chạy |
|---|---|---|---|---|---|
| `POST /api/v1/auth/login` | Được | Được | Được | Không dùng | Login đúng 200; sai password 401. |
| `GET /api/v1|v2/rooms` | 200 khi JWT hợp lệ | 200 | 200 | Lookup nội bộ | No token 401; Student read 200. |
| `POST/PUT/DELETE /api/v1/rooms` | 403 | Được | Được | Không | Student 403; Manager create 201. |
| `POST /api/v1/reservations` | Chỉ cho chính mình | 403 | Có thể tạo theo policy | Không | Spoof body vẫn owner Student01. |
| `GET /api/v1|v2/reservations` | Chỉ own rows | Xem schedule | Xem schedule | Không | Foreign filter không vượt owner scope. |

## 3. Ownership rule

| Tình huống | Request client | Rule server | Kết quả đã chạy |
|---|---|---|---|
| Create spoof | Student01 gửi `requesterId=STUDENT-002`. | `enforceCreateOwnership` lấy `requesterId`/`displayName` từ JWT. | Reservation được ghi với `STUDENT-001`. |
| Foreign filter | Student01 query `requesterId=STUDENT-002`. | `effectiveRequesterIdForRead` ép filter về claim JWT. | Chỉ trả own rows Student01. |
| Student02 isolation | Student02 query list. | Same ownership policy, requesterId khác. | Không thấy Reservation Student01. |

## 4. Source trace

| Nội dung | File chính |
|---|---|
| BCrypt + login | `auth-service/.../AuthService.java`, `AuthDataInitializer.java`, `AuthUserEntity.java` |
| JWT claims | `auth-service/.../JwtTokenService.java` |
| Bearer verify / 401 | Room/Reservation `JwtAuthenticationFilter.java`, `JwtTokenVerifier.java` |
| Role / 403 | Room/Reservation `SecurityConfig.java` |
| Ownership | `reservation-service/.../ReservationAccessPolicy.java`, `ReservationService.java` |

## 5. Kết quả

**Đạt:** login/JWT, 401, 403, Student–Manager RBAC, create/read ownership, Student02 isolation và machine identity đã chạy thành công. Chi tiết runtime nằm trong `test-results.md`.
