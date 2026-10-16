package vn.edu.reservationservice.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import vn.edu.reservationservice.reservation.Reservation;

@Service
public class ReservationEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public ReservationEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishReservationConfirmed(Reservation reservation) {
        ReservationConfirmedEvent event = ReservationConfirmedEvent.from(reservation);

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqTopologyConfig.RESERVATION_EVENTS_EXCHANGE,
                    RabbitMqTopologyConfig.RESERVATION_CONFIRMED_ROUTING_KEY,
                    event
            );

            LOGGER.info(
                    "Published ReservationConfirmedEvent: eventId={}, reservationId={}",
                    event.eventId(),
                    reservation.id()
            );
        } catch (AmqpException exception) {
            LOGGER.error(
                    "Reservation {} was confirmed, but its event could not be published",
                    reservation.id(),
                    exception
            );
        }
    }
}