package vn.edu.reservationservice.reservation;

/**
 * A privacy-safe availability result. It exposes no other requester's
 * identity, purpose, or Reservation records.
 */
public record AvailableRoom(
        Long roomId,
        String roomCode,
        String building,
        Integer floor,
        String roomType,
        Integer capacity,
        Integer totalComputers,
        Integer workingComputers,
        String availability
) {
}
