# KẾT QUẢ KIỂM THỬ BẢO MẬT TH5
## SOA Room Reservation — Kiểm tra số 1

**Môi trường đã chạy:** `feature/th5-auth-security`; Auth `8083`; Room `8080`; Reservation `8081`; Audit `8082`; PostgreSQL và RabbitMQ healthy.

**Evidence artifacts:** collection gốc `postman/TH5_Security.postman_collection.json` giữ demo 01–09; collection `postman/TH5_Security_Extended_Evidence.postman_collection.json` tái hiện thêm Manager Room 201, Reservation time động và Student02 isolation. Audit receipt được xác nhận tại Audit terminal sau Reservation 201.

---

## 1. Đăng nhập, BCrypt và JWT

| Case | Thao tác | Kết quả thực tế | Đánh giá |
|---|---|---|---|
| S01 | Login `student01` đúng password. | HTTP 200; `tokenType=Bearer`; `expiresIn=900`; token Student01 được dùng cho protected API. | **Đạt** |
| S02 | Login `student02` đúng password. | HTTP 200; token Student02 được dùng kiểm isolation. | **Đạt** |
| S03 | Login `roommanager01` đúng password. | HTTP 200; token Manager được dùng cho Room mutation. | **Đạt** |
| S04 | Login Student01 với password sai. | HTTP 401; `INVALID_CREDENTIALS`; không cấp token. | **Đạt** |

**Source đối chiếu:** `AuthUserEntity.passwordHash`; `AuthDataInitializer.passwordEncoder.encode`; `AuthService.passwordEncoder.matches`; `JwtTokenService.issueUserToken`.

**Kết luận mục 1: Đạt.** Login hoạt động, password được xử lý bằng BCrypt và JWT được phát sau authentication thành công.

---

## 2. Protected API và RBAC

| Case | Thao tác | Kết quả thực tế | Đánh giá |
|---|---|---|---|
| S05 | GET Room không Bearer token. | HTTP 401; `AUTHENTICATION_REQUIRED`. | **Đạt** |
| S06 | Student01 GET Room với JWT hợp lệ. | HTTP 200. | **Đạt** |
| S07 | Student01 POST Room với body hợp lệ. | HTTP 403; `ACCESS_DENIED`. | **Đạt** |
| S08 | Room Manager POST Room với code unique. | HTTP 201 Created. | **Đạt** |

**Source đối chiếu:** Room `JwtAuthenticationFilter`, `JwtTokenVerifier`, `SecurityConfig.hasAnyRole("ROOM_MANAGER", "ADMIN")`.

**Kết luận mục 2: Đạt.** API bảo vệ token hoạt động; Student và Room Manager nhận kết quả khác nhau trên cùng action tạo Room.

---

## 3. Ownership của Reservation

| Case | Thao tác | Kết quả thực tế | Đánh giá |
|---|---|---|---|
| S09 | Student01 gửi body `requesterId=STUDENT-002` khi create Reservation. | HTTP 201; response/persisted owner là `STUDENT-001`. | **Đạt** |
| S10 | Student01 query `requesterId=STUDENT-002`. | HTTP 200; content chỉ thuộc `STUDENT-001`. | **Đạt** |
| S11 | Student02 query Reservation sau khi Student01 tạo record. | HTTP 200; không thấy Reservation Student01. | **Đạt** |

**Source đối chiếu:** `ReservationAccessPolicy.enforceCreateOwnership`, `effectiveRequesterIdForRead`, `ReservationService.createReservation`.

**Kết luận mục 3: Đạt.** Server không tin identity từ body/query của Student và giới hạn dữ liệu theo requesterId trong JWT.

---

## 4. Machine identity và Audit

| Case | Thao tác | Kết quả thực tế | Đánh giá |
|---|---|---|---|
| S12 | Reservation tạo Reservation và gọi Room. | Reservation dùng machine JWT `SERVICE_RESERVATION` audience `room-service`; Room lookup thành công. | **Đạt** |
| S13 | Sau Reservation thành công, kiểm terminal Audit. | Audit nhận event `reservation.confirmed` và log receipt. | **Đạt** |

**Source đối chiếu:** `ServiceTokenClient`, `RoomClient`, `JwtTokenService.issueReservationServiceToken`, `ReservationEventPublisher`, `ReservationAuditConsumer`.

**Kết luận mục 4: Đạt.** Service-to-service identity và audit event hoạt động đúng luồng.

---

## KẾT LUẬN CUỐI

| Tiêu chí Câu 2 Ý 4 | Kết quả |
|---|---|
| Login, password safety, JWT validity | **Đạt** |
| Protected API và phân quyền tối thiểu hai role | **Đạt** |
| Ownership / phạm vi truy cập resource | **Đạt** |
| Test hợp lệ, không hợp lệ và kết quả rõ | **Đạt** |

Video trình bày lần lượt: source/configuration → request manual → response thực tế → source rule → Collection Runner. Video là minh chứng chính cho các case trên.
