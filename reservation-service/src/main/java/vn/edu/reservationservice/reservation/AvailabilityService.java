package vn.edu.reservationservice.reservation;

import org.springframework.stereotype.Service;
import vn.edu.reservationservice.client.RoomClient;
import vn.edu.reservationservice.client.RoomDirectoryEntry;
import vn.edu.reservationservice.client.RoomDirectoryPage;
import vn.edu.reservationservice.reservation.persistence.ReservationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reservation Service owns the availability decision because only this service
 * owns confirmed schedule data. It remains a Consumer of Room Service for
 * Room metadata and never reads room_db directly.
 */
@Service
public class AvailabilityService {

    private static final Set<String> SUPPORTED_ROOM_TYPES = Set.of("THEORY", "LAB");

    private final RoomClient roomClient;
    private final ReservationRepository reservationRepository;

    public AvailabilityService(
            RoomClient roomClient,
            ReservationRepository reservationRepository
    ) {
        this.roomClient = roomClient;
        this.reservationRepository = reservationRepository;
    }

    public AvailabilityResponse findAvailability(
            LocalDateTime from,
            LocalDateTime to,
            Integer attendeeCount,
            String requestedRoomType
    ) {
        validate(from, to, attendeeCount);
        String roomType = normalizeRoomType(requestedRoomType);

        RoomDirectoryPage roomPage = roomClient.findAvailableRooms(roomType, attendeeCount);
        List<RoomDirectoryEntry> candidates = roomPage.content();
        Set<Long> conflictedRoomIds = candidates.isEmpty()
                ? Set.of()
                : reservationRepository.findConflictingRoomIds(
                                candidates.stream().map(RoomDirectoryEntry::id).toList(),
                                ReservationStatus.CONFIRMED,
                                from,
                                to
                        )
                        .stream()
                        .collect(Collectors.toUnmodifiableSet());

        List<AvailableRoom> rooms = candidates.stream()
                .map(room -> new AvailableRoom(
                        room.id(),
                        room.code(),
                        room.building(),
                        room.floor(),
                        room.type(),
                        room.capacity(),
                        room.totalComputers(),
                        room.workingComputers(),
                        conflictedRoomIds.contains(room.id())
                                ? "SCHEDULE_CONFLICT"
                                : "AVAILABLE"
                ))
                .toList();

        long availableCount = rooms.stream()
                .filter(room -> "AVAILABLE".equals(room.availability()))
                .count();

        return new AvailabilityResponse(
                from,
                to,
                attendeeCount,
                roomType,
                rooms.size(),
                availableCount,
                rooms
        );
    }

    private void validate(LocalDateTime from, LocalDateTime to, Integer attendeeCount) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from và to là bắt buộc khi tìm phòng trống");
        }
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from phải trước to");
        }
        if (attendeeCount == null || attendeeCount < 1) {
            throw new IllegalArgumentException("attendeeCount phải lớn hơn 0");
        }
    }

    private String normalizeRoomType(String requestedRoomType) {
        if (requestedRoomType == null || requestedRoomType.isBlank()) {
            return null;
        }

        String roomType = requestedRoomType.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_ROOM_TYPES.contains(roomType)) {
            throw new IllegalArgumentException("type chỉ có thể là THEORY hoặc LAB");
        }
        return roomType;
    }
}
