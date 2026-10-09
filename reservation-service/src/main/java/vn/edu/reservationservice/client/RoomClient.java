package vn.edu.reservationservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class RoomClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoomClient.class);

    private final RestClient roomRestClient;

    public RoomClient(RestClient roomRestClient) {
        this.roomRestClient = roomRestClient;
    }

    @Retryable(
            retryFor = {
                    ResourceAccessException.class,
                    HttpServerErrorException.class
            },
            maxAttemptsExpression = "${room.service.retry.max-attempts}",
            backoff = @Backoff(
                    delayExpression = "${room.service.retry.backoff-delay-ms}"
            )
    )
    public RoomResponse getRoomById(Long roomId) {
        LOGGER.info("Calling Room Service to verify roomId={}", roomId);

        try {
            return roomRestClient.get()
                    .uri("/api/v1/rooms/{id}", roomId)
                    .retrieve()
                    .body(RoomResponse.class);
        } catch (HttpClientErrorException.NotFound exception) {
            throw new RoomNotFoundException(roomId);
        }
    }

    @Recover
    public RoomResponse recoverFromRoomServiceFailure(
            RestClientException exception,
            Long roomId
    ) {
        LOGGER.error(
                "Room Service remains unavailable after all retry attempts. roomId={}",
                roomId,
                exception
        );

        throw new RoomServiceUnavailableException(roomId, exception);
    }
}