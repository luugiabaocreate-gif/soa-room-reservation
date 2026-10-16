package vn.edu.reservationservice.reservation;

import org.springframework.stereotype.Service;
import vn.edu.reservationservice.client.RoomClient;
import vn.edu.reservationservice.client.RoomResponse;
import vn.edu.reservationservice.messaging.ReservationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ReservationService {

    private final List<Reservation> reservations = new ArrayList<>();
    private final AtomicLong nextId = new AtomicLong(0);
    private final RoomClient roomClient;
    private final ReservationEventPublisher reservationEventPublisher;

    public ReservationService(
            RoomClient roomClient,
            ReservationEventPublisher reservationEventPublisher
    ) {
        this.roomClient = roomClient;
        this.reservationEventPublisher = reservationEventPublisher;
    }

    public synchronized Reservation createReservation(
            ReservationRequest request
    ) {
        validateTimeRange(request);

        RoomResponse room = roomClient.getRoomById(request.roomId());

        validateRoomAvailability(room);
        validateCapacity(room, request.attendeeCount());
        validateNoTimeConflict(request);

        Reservation reservation = new Reservation(
                nextId.incrementAndGet(),
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

        reservations.add(reservation);
        reservationEventPublisher.publishReservationConfirmed(reservation);

        return reservation;
    }

    public List<Reservation> getAllReservations() {
        return List.copyOf(reservations);
    }

    private void validateTimeRange(ReservationRequest request) {
        if (!request.startTime().isBefore(request.endTime())) {
            throw new InvalidReservationTimeException();
        }
    }

    private void validateRoomAvailability(RoomResponse room) {
        if (!"AVAILABLE".equals(room.status())) {
            throw new RoomUnavailableException(
                    "Phòng " + room.code() + " hiện có trạng thái " + room.status()
            );
        }
    }

    private void validateCapacity(RoomResponse room, Integer attendeeCount) {
        if (attendeeCount > room.capacity()) {
            throw new RoomUnavailableException(
                    "Phòng " + room.code()
                            + " chỉ chứa tối đa " + room.capacity()
                            + " người"
            );
        }
    }

    private void validateNoTimeConflict(ReservationRequest request) {
        boolean hasConflict = reservations.stream()
                .filter(reservation -> reservation.roomId().equals(request.roomId()))
                .filter(reservation -> reservation.status() == ReservationStatus.CONFIRMED)
                .anyMatch(reservation ->
                        request.startTime().isBefore(reservation.endTime())
                                && request.endTime().isAfter(reservation.startTime())
                );

        if (hasConflict) {
            throw new RoomScheduleConflictException(request.roomId());
        }
    }
}