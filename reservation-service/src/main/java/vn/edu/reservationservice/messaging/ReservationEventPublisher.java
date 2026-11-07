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
        publish(
                RabbitMqTopologyConfig.RESERVATION_CONFIRMED_ROUTING_KEY,
                event,
                reservation,
                "confirmed"
        );
    }

    public void publishReservationCancelled(Reservation reservation) {
        ReservationCancelledEvent event = ReservationCancelledEvent.from(reservation);
        publish(
                RabbitMqTopologyConfig.RESERVATION_CANCELLED_ROUTING_KEY,
                event,
                reservation,
                "cancelled"
        );
    }

    private void publish(
            String routingKey,
            Object event,
            Reservation reservation,
            String lifecycleAction
    ) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqTopologyConfig.RESERVATION_EVENTS_EXCHANGE,
                    routingKey,
                    event
            );

            LOGGER.info(
                    "Published reservation lifecycle event: action={}, reservationId={}",
                    lifecycleAction,
                    reservation.id()
            );
        } catch (AmqpException exception) {
            LOGGER.error(
                    "Reservation {} was {}, but its event could not be published",
                    reservation.id(),
                    lifecycleAction,
                    exception
            );
        }
    }
}