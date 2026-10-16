package vn.edu.reservationservice.reservation;

import java.util.List;

public record ReservationPageResponse(
        List<Reservation> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        String sort
) {

    public ReservationPageResponse {
        content = List.copyOf(content);
    }
}
