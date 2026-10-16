package vn.edu.reservationservice.messaging;

import vn.edu.reservationservice.reservation.Reservation;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationConfirmedEvent(
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

    public static ReservationConfirmedEvent from(Reservation reservation) {
        return new ReservationConfirmedEvent(
                UUID.randomUUID().toString(),
                "reservation.confirmed",
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