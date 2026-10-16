package vn.edu.roomservice.room;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/rooms")
@Tag(name = "Room API v2", description = "Query API TH3: search, filter, sort và pagination")
public class RoomQueryV2Controller {

    private final RoomService roomService;

    public RoomQueryV2Controller(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    @Operation(summary = "Tìm kiếm, lọc, sắp xếp và phân trang danh sách phòng")
    public RoomPageResponse getRooms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RoomType type,
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String sort
    ) {
        return roomService.getRooms(
                keyword,
                type,
                status,
                minCapacity,
                page,
                size,
                sort
        );
    }
}
