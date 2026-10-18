package vn.edu.reservationservice.reservation.persistence;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.reservationservice.client.RoomResponse;
import vn.edu.reservationservice.reservation.Reservation;
import vn.edu.reservationservice.reservation.ReservationRequest;
import vn.edu.reservationservice.reservation.ReservationStatus;
import vn.edu.reservationservice.reservation.RoomScheduleConflictException;

@Service
public class ReservationPersistenceService {

    private final ReservationRepository reservationRepository;

    public ReservationPersistenceService(
            ReservationRepository reservationRepository
    ) {
        this.reservationRepository = reservationRepository;
    }

    /**
     * This transaction covers only Reservation Service's own database.
     * Room verification has already completed through REST before this method is called.
     */
    @Transactional
    public Reservation createConfirmedReservation(
            ReservationRequest request,
            RoomResponse room
    ) {
        boolean hasConflict = reservationRepository.existsOverlappingReservation(
                request.roomId(),
                ReservationStatus.CONFIRMED,
                request.startTime(),
                request.endTime()
        );

        if (hasConflict) {
            throw new RoomScheduleConflictException(request.roomId());
        }

        ReservationEntity entity = new ReservationEntity(
                room.id(),
                room.code(),
                request.requesterId().trim(),
                request.requesterName().trim(),
                request.purpose().trim(),
                request.startTime(),
                request.endTime(),
                request.attendeeCount(),
                ReservationStatus.CONFIRMED
        );

        return ReservationMapper.toResponse(reservationRepository.save(entity));
    }
}
