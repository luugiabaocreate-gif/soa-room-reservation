package vn.edu.reservationservice.reservation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v2/reservations")
@Tag(name = "Reservation API v2", description = "Query API TH3: filter, sort và pagination")
public class ReservationQueryV2Controller {

    private final ReservationService reservationService;

    public ReservationQueryV2Controller(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    @Operation(summary = "Lọc, sắp xếp và phân trang các đăng ký phòng")
    public ReservationPageResponse getReservations(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String requesterId,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String sort
    ) {
        return reservationService.getReservations(
                roomId,
                requesterId,
                status,
                from,
                to,
                page,
                size,
                sort
        );
    }
}
