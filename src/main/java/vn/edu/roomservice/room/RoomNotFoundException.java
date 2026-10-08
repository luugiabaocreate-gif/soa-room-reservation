package vn.edu.roomservice.room;

public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException(Long id) {
        super("Không tìm thấy phòng với id=" + id);
    }
}