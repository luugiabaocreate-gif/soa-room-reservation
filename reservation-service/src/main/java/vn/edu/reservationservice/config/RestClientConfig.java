package vn.edu.reservationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean("roomRestClient")
    public RestClient roomRestClient(
            @Value("${room.service.base-url}") String roomServiceBaseUrl,
            @Value("${room.service.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${room.service.read-timeout-ms}") int readTimeoutMs
    ) {
        return restClient(roomServiceBaseUrl, connectTimeoutMs, readTimeoutMs);
    }

    @Bean("authServiceRestClient")
    public RestClient authServiceRestClient(
            @Value("${auth.service.base-url}") String authServiceBaseUrl,
            @Value("${auth.service.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${auth.service.read-timeout-ms}") int readTimeoutMs
    ) {
        return restClient(authServiceBaseUrl, connectTimeoutMs, readTimeoutMs);
    }

    private RestClient restClient(String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}