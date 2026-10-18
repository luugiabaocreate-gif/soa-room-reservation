package vn.edu.roomservice.persistence;

import vn.edu.roomservice.room.Room;

public final class RoomMapper {

    private RoomMapper() {
    }

    public static Room toResponse(RoomEntity entity) {
        return new Room(
                entity.getId(),
                entity.getCode(),
                entity.getBuilding(),
                entity.getFloor(),
                entity.getType(),
                entity.getCapacity(),
                entity.getTotalComputers(),
                entity.getWorkingComputers(),
                entity.getBrokenComputers(),
                entity.getStatus()
        );
    }
}
