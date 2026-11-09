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
@RequestMapping("/api/v3/availability")
@Tag(
        name = "Reservation Availability API v3",
        description = "Composite availability view: Room Provider metadata plus Reservation schedule conflicts"
)
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping
    @Operation(summary = "Tìm phòng có thể đặt trong một khoảng thời gian")
    public AvailabilityResponse findAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam Integer attendeeCount,
            @RequestParam(required = false) String type
    ) {
        return availabilityService.findAvailability(from, to, attendeeCount, type);
    }
}
