package vn.edu.auditservice.messaging;

public record ReservationConfirmedAuditMessage(
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
}