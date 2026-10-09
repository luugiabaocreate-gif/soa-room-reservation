package vn.edu.reservationservice.reservation;

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
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of(
                "message", "Welcome to Reservation Service",
                "status", "UP"
        );
    }

    @GetMapping("/info")
    public Map<String, String> info() {
        return Map.of(
                "service", "Reservation Service",
                "version", "1.0",
                "status", "UP"
        );
    }

    @GetMapping
    public List<Reservation> getAllReservations() {
        return reservationService.getAllReservations();
    }

    @PostMapping
    public ResponseEntity<Reservation> createReservation(
            @Valid @RequestBody ReservationRequest request
    ) {
        Reservation reservation = reservationService.createReservation(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reservation);
    }
}