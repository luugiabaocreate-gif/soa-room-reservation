package vn.edu.reservationservice.client;

public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException(Long roomId) {
        super("Không tìm thấy phòng với id=" + roomId);
    }
}