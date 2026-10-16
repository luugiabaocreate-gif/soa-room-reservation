package vn.edu.roomservice.room;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Room API v1", description = "Contract TH1 được giữ tương thích ngược")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/hello")
    @Operation(summary = "Kiểm tra Room Service đang chạy")
    public Map<String, String> hello() {
        return Map.of(
                "message", "Welcome to Room Service",
                "status", "UP"
        );
    }

    @GetMapping("/info")
    @Operation(summary = "Xem thông tin Room Service")
    public Map<String, String> info() {
        return Map.of(
                "service", "Room Service",
                "version", "1.0",
                "status", "UP"
        );
    }

    /**
     * Contract TH1: vẫn trả raw List để Postman Collection TH1 và client cũ không bị phá.
     * Query phân trang mới nằm ở GET /api/v2/rooms.
     */
    @GetMapping
    @Operation(summary = "Lấy toàn bộ phòng theo contract v1")
    public List<Room> getAllRooms() {
        return roomService.getAllRooms();
    }

    @GetMapping("/search")
    @Operation(summary = "Search TH1 tương thích ngược, trả về danh sách không phân trang")
    public List<Room> searchRooms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RoomType type,
            @RequestParam(required = false) RoomStatus status
    ) {
        return roomService.searchRooms(keyword, type, status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy một phòng theo id")
    public Room getRoomById(@PathVariable Long id) {
        return roomService.getRoomById(id);
    }

    @PostMapping
    @Operation(summary = "Tạo phòng mới")
    public ResponseEntity<Room> createRoom(
            @Valid @RequestBody RoomRequest request
    ) {
        Room createdRoom = roomService.createRoom(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdRoom);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật đầy đủ một phòng")
    public Room updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomRequest request
    ) {
        return roomService.updateRoom(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa một phòng")
    public ResponseEntity<Void> deleteRoom(
            @PathVariable Long id
    ) {
        roomService.deleteRoom(id);

        return ResponseEntity.noContent().build();
    }
}
