package ch.tbz.m321orderservice.messaging;

import ch.tbz.m321orderservice.event.OrderProcessedEvent;
import ch.tbz.m321orderservice.service.OrderProcessedHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderProcessedConsumer {

    private final OrderProcessedHandler handler;

    public OrderProcessedConsumer(OrderProcessedHandler handler) {
        this.handler = handler;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_PROCESSED_QUEUE)
    public void consume(OrderProcessedEvent event) {
        handler.handle(event);
    }
}
