package vn.edu.reservationservice.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqTopologyConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMqTopologyConfig.class);

    public static final String RESERVATION_EVENTS_EXCHANGE = "reservation.events";
    public static final String RESERVATION_AUDIT_QUEUE = "reservation.audit.queue";
    public static final String RESERVATION_CONFIRMED_ROUTING_KEY = "reservation.confirmed";

    @Bean
    public TopicExchange reservationEventsExchange() {
        return new TopicExchange(RESERVATION_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue reservationAuditQueue() {
        return new Queue(RESERVATION_AUDIT_QUEUE, true);
    }

    @Bean
    public Binding reservationConfirmedAuditBinding(
            Queue reservationAuditQueue,
            TopicExchange reservationEventsExchange
    ) {
        return BindingBuilder
                .bind(reservationAuditQueue)
                .to(reservationEventsExchange)
                .with(RESERVATION_CONFIRMED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter("vn.edu.reservationservice.messaging");
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "reservation.messaging.topology",
            name = "declare-on-startup",
            havingValue = "true",
            matchIfMissing = true
    )
    public ApplicationRunner declareReservationTopology(
            AmqpAdmin amqpAdmin,
            TopicExchange reservationEventsExchange,
            Queue reservationAuditQueue,
            Binding reservationConfirmedAuditBinding
    ) {
        return (ApplicationArguments arguments) -> {
            amqpAdmin.declareExchange(reservationEventsExchange);
            amqpAdmin.declareQueue(reservationAuditQueue);
            amqpAdmin.declareBinding(reservationConfirmedAuditBinding);

            LOGGER.info(
                    "RabbitMQ topology declared: exchange={}, queue={}, routingKey={}",
                    RESERVATION_EVENTS_EXCHANGE,
                    RESERVATION_AUDIT_QUEUE,
                    RESERVATION_CONFIRMED_ROUTING_KEY
            );
        };
    }
}