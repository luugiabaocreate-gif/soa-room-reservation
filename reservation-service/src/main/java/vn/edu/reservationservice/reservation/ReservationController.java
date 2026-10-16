package vn.edu.reservationservice.reservation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reservations")
@Tag(name = "Reservation API v1", description = "Contract TH2 được giữ tương thích ngược")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/hello")
    @Operation(summary = "Kiểm tra Reservation Service đang chạy")
    public Map<String, String> hello() {
        return Map.of(
                "message", "Welcome to Reservation Service",
                "status", "UP"
        );
    }

    @GetMapping("/info")
    @Operation(summary = "Xem thông tin Reservation Service")
    public Map<String, String> info() {
        return Map.of(
                "service", "Reservation Service",
                "version", "1.0",
                "status", "UP"
        );
    }

    /**
     * Contract TH2: vẫn trả raw List để Postman Collection TH2 không bị phá.
     * Query phân trang mới nằm ở GET /api/v2/reservations.
     */
    @GetMapping
    @Operation(summary = "Lấy toàn bộ đăng ký theo contract v1")
    public List<Reservation> getAllReservations() {
        return reservationService.getAllReservations();
    }

    @PostMapping
    @Operation(summary = "Tạo đăng ký phòng mới")
    public ResponseEntity<Reservation> createReservation(
            @Valid @RequestBody ReservationRequest request
    ) {
        Reservation reservation = reservationService.createReservation(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reservation);
    }
}
