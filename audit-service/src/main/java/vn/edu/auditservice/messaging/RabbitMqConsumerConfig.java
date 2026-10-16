package vn.edu.auditservice.messaging;

import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConsumerConfig {

    @Bean
    public MessageConverter rabbitMessageConverter() {
        JacksonJsonMessageConverter converter =
                new JacksonJsonMessageConverter("vn.edu.auditservice.messaging");

        converter.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);

        return converter;
    }
}