package vn.edu.reservationservice.client;

import java.util.List;

/** Response shape of Room Service GET /api/v2/rooms. */
public record RoomDirectoryPage(
        List<RoomDirectoryEntry> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        String sort
) {

    public RoomDirectoryPage {
        content = content == null ? List.of() : List.copyOf(content);
    }
}
