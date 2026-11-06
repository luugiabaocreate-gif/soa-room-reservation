package vn.edu.reservationservice.reservation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.reservationservice.client.RoomClient;
import vn.edu.reservationservice.client.RoomResponse;
import vn.edu.reservationservice.messaging.ReservationEventPublisher;
import vn.edu.reservationservice.reservation.persistence.ReservationEntity;
import vn.edu.reservationservice.reservation.persistence.ReservationMapper;
import vn.edu.reservationservice.reservation.persistence.ReservationPersistenceService;
import vn.edu.reservationservice.reservation.persistence.ReservationRepository;
import vn.edu.reservationservice.security.ReservationAccessPolicy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class ReservationService {

    private static final int MAX_PAGE_SIZE = 50;

    private final RoomClient roomClient;
    private final ReservationEventPublisher reservationEventPublisher;
    private final ReservationRepository reservationRepository;
    private final ReservationPersistenceService reservationPersistenceService;
    private final ReservationAccessPolicy reservationAccessPolicy;

    public ReservationService(
            RoomClient roomClient,
            ReservationEventPublisher reservationEventPublisher,
            ReservationRepository reservationRepository,
            ReservationPersistenceService reservationPersistenceService,
            ReservationAccessPolicy reservationAccessPolicy
    ) {
        this.roomClient = roomClient;
        this.reservationEventPublisher = reservationEventPublisher;
        this.reservationRepository = reservationRepository;
        this.reservationPersistenceService = reservationPersistenceService;
        this.reservationAccessPolicy = reservationAccessPolicy;
    }

    public Reservation createReservation(ReservationRequest request) {
        request = reservationAccessPolicy.enforceCreateOwnership(request);
        validateTimeRange(request);

        // Synchronous business verification. This is deliberately outside the local DB transaction.
        RoomResponse room = roomClient.getRoomById(request.roomId());
        validateRoomAvailability(room);
        validateCapacity(room, request.attendeeCount());

        // Transaction begins and ends inside ReservationPersistenceService for reservation_db only.
        Reservation reservation = reservationPersistenceService.createConfirmedReservation(
                request,
                room
        );

        // This is after local persistence. It is not a distributed transaction/outbox yet.
        reservationEventPublisher.publishReservationConfirmed(reservation);

        return reservation;
    }

    @Transactional(readOnly = true)
    public ReservationPageResponse getReservations(
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
        requesterId = reservationAccessPolicy.effectiveRequesterIdForRead(requesterId);

        SortOption sortOption = parseSort(sort);
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortOption.direction(), sortOption.field())
        );

        Page<ReservationEntity> reservations = reservationRepository.findAll(
                buildSpecification(roomId, requesterId, status, from, to),
                pageable
        );

        return new ReservationPageResponse(
                reservations.getContent().stream()
                        .map(ReservationMapper::toResponse)
                        .toList(),
                reservations.getNumber(),
                reservations.getSize(),
                reservations.getTotalElements(),
                reservations.getTotalPages(),
                sortOption.asResponseValue()
        );
    }

    @Transactional(readOnly = true)
    public List<Reservation> getAllReservations() {
        String requesterId = reservationAccessPolicy.effectiveRequesterIdForRead(null);
        return reservationRepository.findAll(
                        buildSpecification(null, requesterId, null, null, null),
                        Pageable.unpaged()
                )
                .getContent()
                .stream()
                .map(ReservationMapper::toResponse)
                .toList();
    }

    /**
     * Add predicates only when a filter is supplied. This keeps PostgreSQL from
     * receiving an untyped null parameter in a lower(...) expression.
     */
    private Specification<ReservationEntity> buildSpecification(
            Long roomId,
            String requesterId,
            ReservationStatus status,
            LocalDateTime from,
            LocalDateTime to
    ) {
        Specification<ReservationEntity> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.conjunction();

        if (roomId != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("roomId"), roomId)
            );
        }

        if (requesterId != null && !requesterId.isBlank()) {
            String pattern = "%" + requesterId.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.<String>get("requesterId")),
                            pattern
                    )
            );
        }

        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status)
            );
        }

        if (from != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThan(root.<LocalDateTime>get("endTime"), from)
            );
        }

        if (to != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThan(root.<LocalDateTime>get("startTime"), to)
            );
        }

        return specification;
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

        return new SortOption(
                field,
                "desc".equals(direction) ? Sort.Direction.DESC : Sort.Direction.ASC
        );
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

    private record SortOption(String field, Sort.Direction direction) {

        private String asResponseValue() {
            return field + "," + direction.name().toLowerCase(Locale.ROOT);
        }
    }
}
