package vn.edu.reservationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient roomRestClient(
            @Value("${room.service.base-url}") String roomServiceBaseUrl,
            @Value("${room.service.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${room.service.read-timeout-ms}") int readTimeoutMs
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);

        return RestClient.builder()
                .baseUrl(roomServiceBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}