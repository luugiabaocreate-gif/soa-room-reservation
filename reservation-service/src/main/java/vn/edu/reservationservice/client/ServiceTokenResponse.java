package vn.edu.reservationservice.client;

public record ServiceTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
