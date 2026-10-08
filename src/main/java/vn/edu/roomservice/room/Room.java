package vn.edu.roomservice.room;

public record Room(
        Long id,
        String code,
        String building,
        Integer floor,
        RoomType type,
        Integer capacity,
        Integer totalComputers,
        Integer workingComputers,
        Integer brokenComputers,
        RoomStatus status
) {
}