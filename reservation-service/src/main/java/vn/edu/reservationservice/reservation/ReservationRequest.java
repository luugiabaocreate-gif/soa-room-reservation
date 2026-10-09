package vn.edu.reservationservice.reservation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record ReservationRequest(
        @NotNull(message = "Id phòng không được để trống")
        @Positive(message = "Id phòng phải lớn hơn 0")
        Long roomId,

        @NotBlank(message = "Mã người đăng ký không được để trống")
        String requesterId,

        @NotBlank(message = "Tên người đăng ký không được để trống")
        String requesterName,

        @NotBlank(message = "Mục đích sử dụng phòng không được để trống")
        String purpose,

        @NotNull(message = "Thời gian bắt đầu không được để trống")
        LocalDateTime startTime,

        @NotNull(message = "Thời gian kết thúc không được để trống")
        LocalDateTime endTime,

        @NotNull(message = "Số người tham gia không được để trống")
        @Min(value = 1, message = "Số người tham gia phải lớn hơn 0")
        Integer attendeeCount
) {
}