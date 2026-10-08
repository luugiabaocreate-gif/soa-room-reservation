package vn.edu.roomservice.room;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RoomService {

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

    public List<Room> getAllRooms() {
        return rooms;
    }

    public Room getRoomById(Long id) {
        return rooms.stream()
                .filter(room -> room.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new RoomNotFoundException(id));
    }

    public List<Room> searchRooms(
            String keyword,
            RoomType type,
            RoomStatus status
    ) {
        String normalizedKeyword = keyword == null
                ? ""
                : keyword.trim().toLowerCase(Locale.ROOT);

        return rooms.stream()
                .filter(room -> matchesKeyword(room, normalizedKeyword))
                .filter(room -> type == null || room.type() == type)
                .filter(room -> status == null || room.status() == status)
                .toList();
    }

    public synchronized Room createRoom(RoomRequest request) {
        String normalizedCode = normalizeCode(request.code());

        boolean codeExists = rooms.stream()
                .anyMatch(room ->
                        room.code().equalsIgnoreCase(normalizedCode)
                );

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
                .anyMatch(room ->
                        !room.id().equals(id)
                                && room.code().equalsIgnoreCase(normalizedCode)
                );

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

    private boolean matchesKeyword(Room room, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }

        return room.code().toLowerCase(Locale.ROOT).contains(keyword)
                || room.building().toLowerCase(Locale.ROOT).contains(keyword);
    }

    private void validateComputerNumbers(RoomRequest request) {
        if (request.type() == RoomType.THEORY
                && request.totalComputers() > 0) {
            throw new IllegalArgumentException(
                    "Phòng THEORY không được khai báo máy thực hành"
            );
        }

        if (request.workingComputers()
                + request.brokenComputers()
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
        return status == null
                ? RoomStatus.AVAILABLE
                : status;
    }

    private RoomStatus getStatusForUpdate(
            RoomStatus requestedStatus,
            RoomStatus currentStatus
    ) {
        return requestedStatus == null
                ? currentStatus
                : requestedStatus;
    }
}