package vn.edu.reservationservice.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class SecurityErrorResponseWriter {

    private final JsonMapper jsonMapper;

    public SecurityErrorResponseWriter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public void writeUnauthorized(
            HttpServletRequest request,
            HttpServletResponse response,
            String message
    ) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", message,
                request.getRequestURI());
    }

    public void writeForbidden(
            HttpServletRequest request,
            HttpServletResponse response,
            String message
    ) throws IOException {
        write(response, HttpStatus.FORBIDDEN, "ACCESS_DENIED", message,
                request.getRequestURI());
    }

    private void write(
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message,
            String path
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "code", code,
                "message", message,
                "path", path
        ));
    }
}
