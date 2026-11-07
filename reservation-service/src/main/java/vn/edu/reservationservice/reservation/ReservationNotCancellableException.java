package vn.edu.reservationservice.reservation;

public class ReservationNotCancellableException extends RuntimeException {

    public ReservationNotCancellableException(Long reservationId, ReservationStatus status) {
        super("Đăng ký id=" + reservationId + " không thể hủy khi có trạng thái " + status);
    }
}
