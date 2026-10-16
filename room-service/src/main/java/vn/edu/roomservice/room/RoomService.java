package vn.edu.roomservice.room;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Service
public class RoomService {

    private static final int MAX_PAGE_SIZE = 50;

    private final List<Room> rooms = new ArrayList<>();

    private final AtomicLong nextId = new AtomicLong(5);

    public RoomService() {
        rooms.add(new Room(
                1L,
                "A101",
                "A",
                1,
                RoomType.THEORY,
                60,
                0,
                0,
                0,
                RoomStatus.AVAILABLE
        ));

        rooms.add(new Room(
                2L,
                "A301",
                "A",
                3,
                RoomType.LAB,
                45,
                45,
                42,
                3,
                RoomStatus.AVAILABLE
        ));

        rooms.add(new Room(
                3L,
                "B201",
                "B",
                2,
                RoomType.THEORY,
                50,
                0,
                0,
                0,
                RoomStatus.MAINTENANCE
        ));

        rooms.add(new Room(
                4L,
                "B305",
                "B",
                3,
                RoomType.LAB,
                40,
                40,
                36,
                4,
                RoomStatus.LOCKED
        ));

        rooms.add(new Room(
                5L,
                "C102",
                "C",
                1,
                RoomType.LAB,
                30,
                30,
                0,
                30,
                RoomStatus.OUT_OF_SERVICE
        ));
    }

    public synchronized List<Room> getAllRooms() {
        return List.copyOf(rooms);
    }

    public synchronized RoomPageResponse getRooms(
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
        List<Room> filteredRooms = filteredRooms(keyword, type, status, minCapacity)
                .sorted(comparatorFor(sortOption))
                .toList();

        long totalElements = filteredRooms.size();
        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / size);

        long fromIndex = (long) page * size;
        List<Room> content;

        if (fromIndex >= totalElements) {
            content = List.of();
        } else {
            int from = (int) fromIndex;
            int to = (int) Math.min(fromIndex + size, totalElements);
            content = filteredRooms.subList(from, to);
        }

        return new RoomPageResponse(
                content,
                page,
                size,
                totalElements,
                totalPages,
                sortOption.asResponseValue()
        );
    }

    public synchronized Room getRoomById(Long id) {
        return rooms.stream()
                .filter(room -> room.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new RoomNotFoundException(id));
    }

    public synchronized List<Room> searchRooms(
            String keyword,
            RoomType type,
            RoomStatus status
    ) {
        return filteredRooms(keyword, type, status, null).toList();
    }

    public synchronized Room createRoom(RoomRequest request) {
        String normalizedCode = normalizeCode(request.code());

        boolean codeExists = rooms.stream()
                .anyMatch(room -> room.code().equalsIgnoreCase(normalizedCode));

        if (codeExists) {
            throw new DuplicateRoomCodeException(normalizedCode);
        }

        validateComputerNumbers(request);

        Long newId = nextId.incrementAndGet();

        Room newRoom = new Room(
                newId,
                normalizedCode,
                request.building().trim(),
                request.floor(),
                request.type(),
                request.capacity(),
                request.totalComputers(),
                request.workingComputers(),
                request.brokenComputers(),
                getStatusForCreate(request.status())
        );

        rooms.add(newRoom);

        return newRoom;
    }

    public synchronized Room updateRoom(
            Long id,
            RoomRequest request
    ) {
        Room existingRoom = getRoomById(id);
        String normalizedCode = normalizeCode(request.code());

        boolean codeExists = rooms.stream()
                .anyMatch(room -> !room.id().equals(id)
                        && room.code().equalsIgnoreCase(normalizedCode));

        if (codeExists) {
            throw new DuplicateRoomCodeException(normalizedCode);
        }

        validateComputerNumbers(request);

        Room updatedRoom = new Room(
                existingRoom.id(),
                normalizedCode,
                request.building().trim(),
                request.floor(),
                request.type(),
                request.capacity(),
                request.totalComputers(),
                request.workingComputers(),
                request.brokenComputers(),
                getStatusForUpdate(request.status(), existingRoom.status())
        );

        int roomIndex = rooms.indexOf(existingRoom);
        rooms.set(roomIndex, updatedRoom);

        return updatedRoom;
    }

    public synchronized void deleteRoom(Long id) {
        Room room = getRoomById(id);
        rooms.remove(room);
    }

    private Stream<Room> filteredRooms(
            String keyword,
            RoomType type,
            RoomStatus status,
            Integer minCapacity
    ) {
        String normalizedKeyword = keyword == null
                ? ""
                : keyword.trim().toLowerCase(Locale.ROOT);

        return rooms.stream()
                .filter(room -> matchesKeyword(room, normalizedKeyword))
                .filter(room -> type == null || room.type() == type)
                .filter(room -> status == null || room.status() == status)
                .filter(room -> minCapacity == null || room.capacity() >= minCapacity);
    }

    private boolean matchesKeyword(Room room, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }

        return room.code().toLowerCase(Locale.ROOT).contains(keyword)
                || room.building().toLowerCase(Locale.ROOT).contains(keyword);
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

        return new SortOption(field, direction);
    }

    private Comparator<Room> comparatorFor(SortOption sortOption) {
        Comparator<Room> comparator = switch (sortOption.field()) {
            case "id" -> Comparator.comparing(Room::id);
            case "code" -> Comparator.comparing(Room::code, String.CASE_INSENSITIVE_ORDER);
            case "building" -> Comparator.comparing(Room::building, String.CASE_INSENSITIVE_ORDER);
            case "floor" -> Comparator.comparing(Room::floor);
            case "capacity" -> Comparator.comparing(Room::capacity);
            case "type" -> Comparator.comparing(room -> room.type().name());
            case "status" -> Comparator.comparing(room -> room.status().name());
            default -> throw new IllegalArgumentException(
                    "sort field không được hỗ trợ: " + sortOption.field()
            );
        };

        return "desc".equals(sortOption.direction())
                ? comparator.reversed()
                : comparator;
    }

    private void validateComputerNumbers(RoomRequest request) {
        if (request.type() == RoomType.THEORY
                && request.totalComputers() > 0) {
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

    private RoomStatus getStatusForCreate(RoomStatus status) {
        return status == null ? RoomStatus.AVAILABLE : status;
    }

    private RoomStatus getStatusForUpdate(
            RoomStatus requestedStatus,
            RoomStatus currentStatus
    ) {
        return requestedStatus == null ? currentStatus : requestedStatus;
    }

    private record SortOption(String field, String direction) {

        private String asResponseValue() {
            return field + "," + direction;
        }
    }
}
