package vn.edu.reservationservice.reservation;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Composite read model owned by Reservation Service: Room metadata is fetched
 * from the Provider, while schedule conflict data remains in reservation_db.
 */
public record AvailabilityResponse(
        LocalDateTime from,
        LocalDateTime to,
        Integer attendeeCount,
        String roomType,
        long candidateCount,
        long availableCount,
        List<AvailableRoom> rooms
) {

    public AvailabilityResponse {
        rooms = List.copyOf(rooms);
    }
}
