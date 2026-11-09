package vn.edu.reservationservice.reservation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.reservationservice.client.RoomClient;
import vn.edu.reservationservice.client.RoomDirectoryEntry;
import vn.edu.reservationservice.client.RoomDirectoryPage;
import vn.edu.reservationservice.reservation.persistence.ReservationRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private RoomClient roomClient;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private AvailabilityService availabilityService;

    @Test
    void availability_keepsProviderMetadataButMasksOtherReservationDetails() {
        LocalDateTime from = LocalDateTime.of(2026, 12, 20, 9, 0);
        LocalDateTime to = LocalDateTime.of(2026, 12, 20, 10, 0);
        when(roomClient.findAvailableRooms("LAB", 20)).thenReturn(new RoomDirectoryPage(
                List.of(
                        room(1L, "A301", "LAB"),
                        room(2L, "B305", "LAB")
                ),
                0,
                50,
                2,
                1,
                "code,asc"
        ));
        when(reservationRepository.findConflictingRoomIds(any(), eq(ReservationStatus.CONFIRMED), eq(from), eq(to)))
                .thenReturn(List.of(2L));

        AvailabilityResponse response = availabilityService.findAvailability(from, to, 20, "lab");

        assertEquals(2, response.candidateCount());
        assertEquals(1, response.availableCount());
        assertEquals("AVAILABLE", response.rooms().get(0).availability());
        assertEquals("SCHEDULE_CONFLICT", response.rooms().get(1).availability());
        assertEquals("A301", response.rooms().get(0).roomCode());
    }

    @Test
    void availability_rejectsInvalidTimeRangeBeforeCallingProvider() {
        LocalDateTime from = LocalDateTime.of(2026, 12, 20, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 12, 20, 9, 0);

        assertThrows(IllegalArgumentException.class,
                () -> availabilityService.findAvailability(from, to, 20, "LAB"));
    }

    private RoomDirectoryEntry room(Long id, String code, String type) {
        return new RoomDirectoryEntry(
                id,
                code,
                "A",
                3,
                type,
                45,
                45,
                42,
                3,
                "AVAILABLE"
        );
    }
}
