package vn.edu.reservationservice.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Service
public class ServiceTokenClient {

    private final RestClient authServiceRestClient;
    private final String clientId;
    private final String clientSecret;

    private volatile CachedToken cachedToken;

    public ServiceTokenClient(
            @Qualifier("authServiceRestClient") RestClient authServiceRestClient,
            @Value("${auth.service.client-id}") String clientId,
            @Value("${auth.service.client-secret}") String clientSecret
    ) {
        this.authServiceRestClient = authServiceRestClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String roomServiceToken() {
        CachedToken current = cachedToken;
        if (current != null && current.isUsable()) {
            return current.value();
        }

        synchronized (this) {
            current = cachedToken;
            if (current != null && current.isUsable()) {
                return current.value();
            }

            if (clientSecret == null || clientSecret.isBlank()) {
                throw new IllegalStateException(
                        "RESERVATION_SERVICE_CLIENT_SECRET chưa được cấu hình cho Reservation Service"
                );
            }

            ServiceTokenResponse response = authServiceRestClient.post()
                    .uri("/api/v1/auth/service-token")
                    .body(new ServiceTokenRequest(clientId, clientSecret))
                    .retrieve()
                    .body(ServiceTokenResponse.class);

            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new IllegalStateException("Auth Service không trả service token hợp lệ");
            }

            cachedToken = new CachedToken(
                    response.accessToken(),
                    Instant.now().plusSeconds(Math.max(1, response.expiresIn() - 30))
            );
            return cachedToken.value();
        }
    }

    private record CachedToken(String value, Instant usableUntil) {
        private boolean isUsable() {
            return Instant.now().isBefore(usableUntil);
        }
    }
}
