package vn.edu.authservice.auth;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
