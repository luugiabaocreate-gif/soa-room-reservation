package vn.edu.roomservice.room;

public class DuplicateRoomCodeException extends RuntimeException {

    public DuplicateRoomCodeException(String code) {
        super("Mã phòng đã tồn tại: " + code);
    }
}