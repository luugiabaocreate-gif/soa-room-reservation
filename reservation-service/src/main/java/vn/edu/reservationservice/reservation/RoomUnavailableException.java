package vn.edu.reservationservice.reservation;

public class RoomUnavailableException extends RuntimeException {

    public RoomUnavailableException(String message) {
        super(message);
    }
}