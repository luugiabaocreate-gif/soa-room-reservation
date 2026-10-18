package vn.edu.reservationservice.reservation.persistence;

import vn.edu.reservationservice.reservation.Reservation;

public final class ReservationMapper {

    private ReservationMapper() {
    }

    public static Reservation toResponse(ReservationEntity entity) {
        return new Reservation(
                entity.getId(),
                entity.getRoomId(),
                entity.getRoomCode(),
                entity.getRequesterId(),
                entity.getRequesterName(),
                entity.getPurpose(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getAttendeeCount(),
                entity.getStatus()
        );
    }
}
