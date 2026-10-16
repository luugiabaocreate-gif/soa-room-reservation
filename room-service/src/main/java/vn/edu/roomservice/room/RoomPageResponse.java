package vn.edu.roomservice.room;

import java.util.List;

public record RoomPageResponse(
        List<Room> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        String sort
) {

    public RoomPageResponse {
        content = List.copyOf(content);
    }
}
