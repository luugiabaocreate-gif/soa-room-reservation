package vn.edu.roomservice.room;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of(
                "message", "Welcome to Room Service",
                "status", "UP"
        );
    }

    @GetMapping("/info")
    public Map<String, String> info() {
        return Map.of(
                "service", "Room Service",
                "version", "1.0",
                "status", "UP"
        );
    }

    @GetMapping
    public List<Room> getAllRooms() {
        return roomService.getAllRooms();
    }

    @GetMapping("/search")
    public List<Room> searchRooms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RoomType type,
            @RequestParam(required = false) RoomStatus status
    ) {
        return roomService.searchRooms(keyword, type, status);
    }

    @GetMapping("/{id}")
    public Room getRoomById(@PathVariable Long id) {
        return roomService.getRoomById(id);
    }

    @PostMapping
    public ResponseEntity<Room> createRoom(
            @Valid @RequestBody RoomRequest request
    ) {
        Room createdRoom = roomService.createRoom(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdRoom);
    }

    @PutMapping("/{id}")
    public Room updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomRequest request
    ) {
        return roomService.updateRoom(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(
            @PathVariable Long id
    ) {
        roomService.deleteRoom(id);

        return ResponseEntity.noContent().build();
    }
}