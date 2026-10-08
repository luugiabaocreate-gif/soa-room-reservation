package vn.edu.roomservice.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.edu.roomservice.room.DuplicateRoomCodeException;
import vn.edu.roomservice.room.RoomNotFoundException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            RoomNotFoundException exception
    ) {
        Map<String, Object> response = Map.of(
                "status", 404,
                "code", "ROOM_NOT_FOUND",
                "message", exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(DuplicateRoomCodeException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateRoomCode(
            DuplicateRoomCodeException exception
    ) {
        Map<String, Object> response = Map.of(
                "status", 409,
                "code", "DUPLICATE_ROOM_CODE",
                "message", exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(
            IllegalArgumentException exception
    ) {
        Map<String, Object> response = Map.of(
                "status", 400,
                "code", "BAD_REQUEST",
                "message", exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(
            MethodArgumentNotValidException exception
    ) {
        List<String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        Map<String, Object> response = Map.of(
                "status", 400,
                "code", "VALIDATION_ERROR",
                "message", "Dữ liệu đầu vào không hợp lệ",
                "errors", errors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
}