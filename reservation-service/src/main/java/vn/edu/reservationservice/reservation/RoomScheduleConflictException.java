package vn.edu.reservationservice.reservation;

public class RoomScheduleConflictException extends RuntimeException {

    public RoomScheduleConflictException(Long roomId) {
        super("Phòng có id=" + roomId + " đã có đăng ký trùng khung giờ");
    }
}