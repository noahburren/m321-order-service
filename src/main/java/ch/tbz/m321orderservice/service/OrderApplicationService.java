package ch.tbz.m321orderservice.service;

import ch.tbz.m321orderservice.api.OrderAcceptedResponse;
import ch.tbz.m321orderservice.event.OrderCreatedEvent;
import ch.tbz.m321orderservice.messaging.OrderEventPublisher;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(OrderApplicationService.class);
    private final OrderEventPublisher eventPublisher;

    public OrderApplicationService(OrderEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public OrderAcceptedResponse accept(String product, int quantity, String correlationId) {
        UUID orderId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(), correlationId, orderId, product, quantity, createdAt, 1);
        eventPublisher.publish(event);
        logger.info("Order accepted and OrderCreated published: orderId={}, eventId={}, product={}, quantity={}",
                orderId, event.eventId(), product, quantity);
        return new OrderAcceptedResponse(orderId, correlationId, "ACCEPTED", createdAt);
    }
}
