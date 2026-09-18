package ch.tbz.m321orderservice.messaging;

import ch.tbz.m321orderservice.event.OrderCreatedEvent;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(OrderCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDERS_EVENTS_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                event,
                message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setCorrelationId(event.correlationId());
                    message.getMessageProperties().setHeader("eventType", "OrderCreated");
                    message.getMessageProperties().setHeader("eventVersion", event.eventVersion());
                    return message;
                });
    }
}
