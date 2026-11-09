package vn.edu.reservationservice.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.URI;

/**
 * TH7 outbound clients. A logical base URL such as http://ROOM-SERVICE is
 * resolved by Eureka and Spring Cloud LoadBalancer before the HTTP request is
 * sent. The user JWT is still never forwarded by the callers themselves.
 */
@Configuration
public class RestClientConfig {

    @Bean("roomLoadBalancedRestClientBuilder")
    @LoadBalanced
    RestClient.Builder roomLoadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean("authLoadBalancedRestClientBuilder")
    @LoadBalanced
    RestClient.Builder authLoadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean("roomRestClient")
    RestClient roomRestClient(
            @Qualifier("roomLoadBalancedRestClientBuilder") RestClient.Builder builder,
            @Value("${room.service.base-url}") String roomServiceBaseUrl,
            @Value("${room.service.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${room.service.read-timeout-ms}") int readTimeoutMs
    ) {
        return builderForLogicalServiceId(roomServiceBaseUrl, "ROOM-SERVICE", builder)
                .baseUrl(roomServiceBaseUrl)
                .requestFactory(requestFactory(connectTimeoutMs, readTimeoutMs))
                .build();
    }

    @Bean("authServiceRestClient")
    RestClient authServiceRestClient(
            @Qualifier("authLoadBalancedRestClientBuilder") RestClient.Builder builder,
            @Value("${auth.service.base-url}") String authServiceBaseUrl,
            @Value("${auth.service.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${auth.service.read-timeout-ms}") int readTimeoutMs
    ) {
        return builderForLogicalServiceId(authServiceBaseUrl, "AUTH-SERVICE", builder)
                .baseUrl(authServiceBaseUrl)
                .requestFactory(requestFactory(connectTimeoutMs, readTimeoutMs))
                .build();
    }

    /**
     * The normal path uses a logical Service ID and therefore must be
     * intercepted by Spring Cloud LoadBalancer. A deliberately supplied
     * host:port override is a local diagnostic escape hatch and must stay a
     * plain RestClient; otherwise LoadBalancer would incorrectly try to look
     * up "localhost" (or the physical host) in Eureka.
     */
    private RestClient.Builder builderForLogicalServiceId(
            String baseUrl,
            String expectedServiceId,
            RestClient.Builder loadBalancedBuilder
    ) {
        URI uri = URI.create(baseUrl);
        if (expectedServiceId.equalsIgnoreCase(uri.getHost()) && uri.getPort() == -1) {
            return loadBalancedBuilder;
        }
        return RestClient.builder();
    }

    private SimpleClientHttpRequestFactory requestFactory(int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        return requestFactory;
    }
}
