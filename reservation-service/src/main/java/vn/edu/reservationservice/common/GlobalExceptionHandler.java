package vn.edu.reservationservice.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.edu.reservationservice.client.RoomNotFoundException;
import vn.edu.reservationservice.client.RoomServiceUnavailableException;
import vn.edu.reservationservice.reservation.InvalidReservationTimeException;
import vn.edu.reservationservice.reservation.RoomScheduleConflictException;
import vn.edu.reservationservice.reservation.RoomUnavailableException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        Map<String, Object> response = Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "code", "VALIDATION_ERROR",
                "message", "Dữ liệu đầu vào không hợp lệ",
                "errors", errors,
                "path", request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(InvalidReservationTimeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidReservationTime(
            InvalidReservationTimeException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                "INVALID_RESERVATION_TIME",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRoomNotFound(
            RoomNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.NOT_FOUND,
                "ROOM_NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(RoomServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleRoomServiceUnavailable(
            RoomServiceUnavailableException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.SERVICE_UNAVAILABLE,
                "ROOM_SERVICE_UNAVAILABLE",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(RoomUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleRoomUnavailable(
            RoomUnavailableException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.CONFLICT,
                "ROOM_NOT_AVAILABLE",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(RoomScheduleConflictException.class)
    public ResponseEntity<Map<String, Object>> handleScheduleConflict(
            RoomScheduleConflictException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.CONFLICT,
                "ROOM_SCHEDULE_CONFLICT",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    private ResponseEntity<Map<String, Object>> buildError(
            HttpStatus status,
            String code,
            String message,
            String path
    ) {
        Map<String, Object> response = Map.of(
                "status", status.value(),
                "code", code,
                "message", message,
                "path", path
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }
}