package vn.edu.reservationservice.messaging;

import vn.edu.reservationservice.reservation.Reservation;

import java.time.LocalDateTime;
import java.util.UUID;

/** Event emitted after a confirmed Reservation is cancelled. */
public record ReservationCancelledEvent(
        String eventId,
        String eventType,
        String eventVersion,
        String occurredAt,
        Long reservationId,
        Long roomId,
        String roomCode,
        String requesterId,
        String requesterName,
        String purpose,
        String startTime,
        String endTime,
        Integer attendeeCount,
        String reservationStatus
) {

    public static ReservationCancelledEvent from(Reservation reservation) {
        return new ReservationCancelledEvent(
                UUID.randomUUID().toString(),
                "reservation.cancelled",
                "1",
                LocalDateTime.now().toString(),
                reservation.id(),
                reservation.roomId(),
                reservation.roomCode(),
                reservation.requesterId(),
                reservation.requesterName(),
                reservation.purpose(),
                reservation.startTime().toString(),
                reservation.endTime().toString(),
                reservation.attendeeCount(),
                reservation.status().name()
        );
    }
}
