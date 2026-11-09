package vn.edu.reservationservice.reservation;

public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(Long reservationId) {
        super("Không tìm thấy đăng ký có id=" + reservationId);
    }
}
