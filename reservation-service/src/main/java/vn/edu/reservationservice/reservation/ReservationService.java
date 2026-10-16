package vn.edu.reservationservice.reservation;

import org.springframework.stereotype.Service;
import vn.edu.reservationservice.client.RoomClient;
import vn.edu.reservationservice.client.RoomResponse;
import vn.edu.reservationservice.messaging.ReservationEventPublisher;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Service
public class ReservationService {

    private static final int MAX_PAGE_SIZE = 50;

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

    public synchronized ReservationPageResponse getReservations(
            Long roomId,
            String requesterId,
            ReservationStatus status,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size,
            String sort
    ) {
        validateQuery(roomId, from, to, page, size);

        SortOption sortOption = parseSort(sort);
        List<Reservation> filteredReservations = filteredReservations(
                roomId,
                requesterId,
                status,
                from,
                to
        ).sorted(comparatorFor(sortOption)).toList();

        long totalElements = filteredReservations.size();
        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / size);
        long fromIndex = (long) page * size;
        List<Reservation> content;

        if (fromIndex >= totalElements) {
            content = List.of();
        } else {
            int fromIndexAsInt = (int) fromIndex;
            int toIndex = (int) Math.min(fromIndex + size, totalElements);
            content = filteredReservations.subList(fromIndexAsInt, toIndex);
        }

        return new ReservationPageResponse(
                content,
                page,
                size,
                totalElements,
                totalPages,
                sortOption.asResponseValue()
        );
    }

    public synchronized List<Reservation> getAllReservations() {
        return List.copyOf(reservations);
    }

    private Stream<Reservation> filteredReservations(
            Long roomId,
            String requesterId,
            ReservationStatus status,
            LocalDateTime from,
            LocalDateTime to
    ) {
        String normalizedRequesterId = requesterId == null
                ? ""
                : requesterId.trim().toLowerCase(Locale.ROOT);

        return reservations.stream()
                .filter(reservation -> roomId == null || reservation.roomId().equals(roomId))
                .filter(reservation -> normalizedRequesterId.isBlank()
                        || reservation.requesterId().toLowerCase(Locale.ROOT)
                        .contains(normalizedRequesterId))
                .filter(reservation -> status == null || reservation.status() == status)
                .filter(reservation -> from == null
                        || reservation.endTime().isAfter(from))
                .filter(reservation -> to == null
                        || reservation.startTime().isBefore(to));
    }

    private void validateQuery(
            Long roomId,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    ) {
        if (roomId != null && roomId <= 0) {
            throw new IllegalArgumentException("roomId phải lớn hơn 0");
        }

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from không được sau to");
        }

        if (page < 0) {
            throw new IllegalArgumentException("page phải lớn hơn hoặc bằng 0");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "size phải trong khoảng từ 1 đến " + MAX_PAGE_SIZE
            );
        }
    }

    private SortOption parseSort(String sort) {
        String value = sort == null || sort.isBlank() ? "id,asc" : sort.trim();
        String[] parts = value.split(",", -1);

        if (parts.length > 2 || parts[0].isBlank()) {
            throw new IllegalArgumentException(
                    "sort phải có dạng field,asc hoặc field,desc"
            );
        }

        String field = parts[0].trim();
        String direction = parts.length == 2
                ? parts[1].trim().toLowerCase(Locale.ROOT)
                : "asc";

        if (!List.of(
                "id",
                "roomId",
                "roomCode",
                "requesterId",
                "startTime",
                "endTime",
                "attendeeCount",
                "status"
        ).contains(field)) {
            throw new IllegalArgumentException("sort field không được hỗ trợ: " + field);
        }

        if (!List.of("asc", "desc").contains(direction)) {
            throw new IllegalArgumentException("sort direction chỉ có thể là asc hoặc desc");
        }

        return new SortOption(field, direction);
    }

    private Comparator<Reservation> comparatorFor(SortOption sortOption) {
        Comparator<Reservation> comparator = switch (sortOption.field()) {
            case "id" -> Comparator.comparing(Reservation::id);
            case "roomId" -> Comparator.comparing(Reservation::roomId);
            case "roomCode" -> Comparator.comparing(
                    Reservation::roomCode,
                    String.CASE_INSENSITIVE_ORDER
            );
            case "requesterId" -> Comparator.comparing(
                    Reservation::requesterId,
                    String.CASE_INSENSITIVE_ORDER
            );
            case "startTime" -> Comparator.comparing(Reservation::startTime);
            case "endTime" -> Comparator.comparing(Reservation::endTime);
            case "attendeeCount" -> Comparator.comparing(Reservation::attendeeCount);
            case "status" -> Comparator.comparing(reservation -> reservation.status().name());
            default -> throw new IllegalArgumentException(
                    "sort field không được hỗ trợ: " + sortOption.field()
            );
        };

        return "desc".equals(sortOption.direction())
                ? comparator.reversed()
                : comparator;
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
                .anyMatch(reservation -> request.startTime().isBefore(reservation.endTime())
                        && request.endTime().isAfter(reservation.startTime()));

        if (hasConflict) {
            throw new RoomScheduleConflictException(request.roomId());
        }
    }

    private record SortOption(String field, String direction) {

        private String asResponseValue() {
            return field + "," + direction;
        }
    }
}
