package vn.edu.roomservice.room;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomRequest(

        @NotBlank(message = "Mã phòng không được để trống")
        String code,

        @NotBlank(message = "Tên tòa không được để trống")
        String building,

        @NotNull(message = "Tầng không được để trống")
        @Min(value = 0, message = "Tầng phải lớn hơn hoặc bằng 0")
        Integer floor,

        @NotNull(message = "Loại phòng không được để trống")
        RoomType type,

        @NotNull(message = "Sức chứa không được để trống")
        @Min(value = 1, message = "Sức chứa phải lớn hơn 0")
        Integer capacity,

        @NotNull(message = "Tổng số máy không được để trống")
        @Min(value = 0, message = "Tổng số máy không được âm")
        Integer totalComputers,

        @NotNull(message = "Số máy hoạt động không được để trống")
        @Min(value = 0, message = "Số máy hoạt động không được âm")
        Integer workingComputers,

        @NotNull(message = "Số máy hỏng không được để trống")
        @Min(value = 0, message = "Số máy hỏng không được âm")
        Integer brokenComputers,

        RoomStatus status
) {
}