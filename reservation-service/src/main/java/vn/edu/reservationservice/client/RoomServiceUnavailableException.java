package vn.edu.reservationservice.client;

public class RoomServiceUnavailableException extends RuntimeException {

    public RoomServiceUnavailableException(Long roomId, Throwable cause) {
        super(
                "Không thể xác minh phòng có id " + roomId
                        + " vì Room Service đang tạm thời không phản hồi. "
                        + "Vui lòng thử lại sau.",
                cause
        );
    }
}