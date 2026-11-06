package vn.edu.authservice.auth;

import jakarta.validation.constraints.NotBlank;

public record ServiceTokenRequest(
        @NotBlank(message = "clientId không được để trống") String clientId,
        @NotBlank(message = "clientSecret không được để trống") String clientSecret
) {
}
