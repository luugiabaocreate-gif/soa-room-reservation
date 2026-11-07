package vn.edu.reservationservice.client;

public class RoomServiceUnavailableException extends RuntimeException {

    public RoomServiceUnavailableException(Long roomId, Throwable cause) {
        this("xác minh phòng có id " + roomId, cause);
    }

    public RoomServiceUnavailableException(String operation, Throwable cause) {
        super(
                "Không thể " + operation
                        + " vì Room Service đang tạm thời không phản hồi. "
                        + "Vui lòng thử lại sau.",
                cause
        );
    }
}