package vn.edu.roomservice.room;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.roomservice.persistence.RoomEntity;
import vn.edu.roomservice.persistence.RoomMapper;
import vn.edu.roomservice.persistence.RoomRepository;

import java.util.List;
import java.util.Locale;

@Service
public class RoomService {

    private static final int MAX_PAGE_SIZE = 50;

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    public List<Room> getAllRooms() {
        return roomRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(RoomMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomPageResponse getRooms(
            String keyword,
            RoomType type,
            RoomStatus status,
            Integer minCapacity,
            int page,
            int size,
            String sort
    ) {
        validateQuery(page, size, minCapacity);

        SortOption sortOption = parseSort(sort);
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortOption.direction(), sortOption.field())
        );

        Page<RoomEntity> rooms = roomRepository.findAll(
                buildSpecification(keyword, type, status, minCapacity),
                pageable
        );

        return new RoomPageResponse(
                rooms.getContent().stream().map(RoomMapper::toResponse).toList(),
                rooms.getNumber(),
                rooms.getSize(),
                rooms.getTotalElements(),
                rooms.getTotalPages(),
                sortOption.asResponseValue()
        );
    }

    @Transactional(readOnly = true)
    public Room getRoomById(Long id) {
        return RoomMapper.toResponse(findRoomEntity(id));
    }

    @Transactional(readOnly = true)
    public List<Room> searchRooms(
            String keyword,
            RoomType type,
            RoomStatus status
    ) {
        return roomRepository.findAll(
                        buildSpecification(keyword, type, status, null),
                        Pageable.unpaged()
                )
                .getContent()
                .stream()
                .map(RoomMapper::toResponse)
                .toList();
    }

    @Transactional
    public Room createRoom(RoomRequest request) {
        String normalizedCode = normalizeCode(request.code());

        if (roomRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new DuplicateRoomCodeException(normalizedCode);
        }

        validateComputerNumbers(request);

        RoomEntity entity = new RoomEntity(
                normalizedCode,
                request.building().trim(),
                request.floor(),
                request.type(),
                request.capacity(),
                request.totalComputers(),
                request.workingComputers(),
                request.brokenComputers(),
                statusForCreate(request.status())
        );

        return RoomMapper.toResponse(roomRepository.save(entity));
    }

    @Transactional
    public Room updateRoom(Long id, RoomRequest request) {
        RoomEntity entity = findRoomEntity(id);
        String normalizedCode = normalizeCode(request.code());

        if (roomRepository.existsByCodeIgnoreCaseAndIdNot(normalizedCode, id)) {
            throw new DuplicateRoomCodeException(normalizedCode);
        }

        validateComputerNumbers(request);

        entity.update(
                normalizedCode,
                request.building().trim(),
                request.floor(),
                request.type(),
                request.capacity(),
                request.totalComputers(),
                request.workingComputers(),
                request.brokenComputers(),
                statusForUpdate(request.status(), entity.getStatus())
        );

        return RoomMapper.toResponse(roomRepository.save(entity));
    }

    @Transactional
    public void deleteRoom(Long id) {
        roomRepository.delete(findRoomEntity(id));
    }

    /**
     * Build predicates only for parameters that the caller actually supplied.
     * This avoids binding a null String parameter as PostgreSQL bytea in JPQL.
     */
    private Specification<RoomEntity> buildSpecification(
            String keyword,
            RoomType type,
            RoomStatus status,
            Integer minCapacity
    ) {
        Specification<RoomEntity> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.conjunction();

        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.or(
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(root.<String>get("code")),
                                    pattern
                            ),
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(root.<String>get("building")),
                                    pattern
                            )
                    )
            );
        }

        if (type != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("type"), type)
            );
        }

        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status)
            );
        }

        if (minCapacity != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(
                            root.<Integer>get("capacity"),
                            minCapacity
                    )
            );
        }

        return specification;
    }

    private RoomEntity findRoomEntity(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException(id));
    }

    private void validateQuery(int page, int size, Integer minCapacity) {
        if (page < 0) {
            throw new IllegalArgumentException("page phải lớn hơn hoặc bằng 0");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "size phải trong khoảng từ 1 đến " + MAX_PAGE_SIZE
            );
        }

        if (minCapacity != null && minCapacity < 1) {
            throw new IllegalArgumentException("minCapacity phải lớn hơn 0");
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

        if (!List.of("id", "code", "building", "floor", "capacity", "type", "status")
                .contains(field)) {
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

    private void validateComputerNumbers(RoomRequest request) {
        if (request.type() == RoomType.THEORY && request.totalComputers() > 0) {
            throw new IllegalArgumentException(
                    "Phòng THEORY không được khai báo máy thực hành"
            );
        }

        if (request.workingComputers() + request.brokenComputers()
                > request.totalComputers()) {
            throw new IllegalArgumentException(
                    "Số máy hoạt động cộng máy hỏng vượt tổng số máy"
            );
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private RoomStatus statusForCreate(RoomStatus status) {
        return status == null ? RoomStatus.AVAILABLE : status;
    }

    private RoomStatus statusForUpdate(
            RoomStatus requestedStatus,
            RoomStatus currentStatus
    ) {
        return requestedStatus == null ? currentStatus : requestedStatus;
    }

    private record SortOption(String field, Sort.Direction direction) {

        private String asResponseValue() {
            return field + "," + direction.name().toLowerCase(Locale.ROOT);
        }
    }
}
