package vn.edu.reservationservice.reservation;

public class InvalidReservationTimeException extends RuntimeException {

    public InvalidReservationTimeException() {
        super("Thời gian kết thúc phải sau thời gian bắt đầu");
    }
}