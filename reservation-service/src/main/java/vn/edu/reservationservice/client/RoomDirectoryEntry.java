package vn.edu.reservationservice.client;

/**
 * Read-only provider representation used only while building the Reservation
 * Service availability view. Room Service remains the owner of this data.
 */
public record RoomDirectoryEntry(
        Long id,
        String code,
        String building,
        Integer floor,
        String type,
        Integer capacity,
        Integer totalComputers,
        Integer workingComputers,
        Integer brokenComputers,
        String status
) {
}
