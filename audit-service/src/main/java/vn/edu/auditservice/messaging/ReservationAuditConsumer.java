package vn.edu.auditservice.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReservationAuditConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationAuditConsumer.class);

    @RabbitListener(queues = "reservation.audit.queue")
    public void consumeReservationConfirmed(
            ReservationConfirmedAuditMessage event
    ) {
        LOGGER.info(
                "AUDIT EVENT RECEIVED: eventId={}, type={}, reservationId={}, roomCode={}, requesterId={}, status={}",
                event.eventId(),
                event.eventType(),
                event.reservationId(),
                event.roomCode(),
                event.requesterId(),
                event.reservationStatus()
        );
    }
}