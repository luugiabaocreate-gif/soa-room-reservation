package vn.edu.reservationservice.discovery;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.reservationservice.client.RoomClient;
import vn.edu.reservationservice.client.RoomProviderInstance;

/**
 * Read-only TH7 diagnostic. Reservation Service is the Consumer; Room Service
 * is resolved by Service ID through Eureka and LoadBalancer with the same
 * machine-token authorization used for Reservation-to-Room integration.
 */
@RestController
@RequestMapping("/api/v1/discovery")
@Tag(name = "TH7 Service Discovery", description = "Observed provider instance chosen through Eureka")
public class RoomProviderDiscoveryController {

    private final RoomClient roomClient;

    public RoomProviderDiscoveryController(RoomClient roomClient) {
        this.roomClient = roomClient;
    }

    @GetMapping("/room-provider")
    @Operation(summary = "Resolve ROOM-SERVICE and return the serving instance identity")
    public RoomProviderInstance discoverRoomProvider() {
        return roomClient.discoverServingRoomInstance();
    }
}
