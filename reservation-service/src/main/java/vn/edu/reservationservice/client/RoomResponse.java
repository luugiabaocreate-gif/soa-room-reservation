package vn.edu.reservationservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RoomResponse(
        Long id,
        String code,
        Integer capacity,
        String status
) {
}