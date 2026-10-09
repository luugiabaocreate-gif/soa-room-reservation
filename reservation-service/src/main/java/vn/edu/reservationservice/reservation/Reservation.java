package vn.edu.reservationservice.reservation;

import java.time.LocalDateTime;

public record Reservation(
        Long id,
        Long roomId,
        String roomCode,
        String requesterId,
        String requesterName,
        String purpose,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer attendeeCount,
        ReservationStatus status
) {
}