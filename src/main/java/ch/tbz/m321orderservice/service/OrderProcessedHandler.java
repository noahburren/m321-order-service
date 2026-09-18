package ch.tbz.m321orderservice.service;

import ch.tbz.m321orderservice.event.OrderProcessedEvent;
import ch.tbz.m321orderservice.idempotency.EventIdempotencyStore;
import ch.tbz.m321orderservice.observability.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class OrderProcessedHandler {

    private static final Logger logger = LoggerFactory.getLogger(OrderProcessedHandler.class);
    private final EventIdempotencyStore idempotencyStore;

    public OrderProcessedHandler(EventIdempotencyStore idempotencyStore) {
        this.idempotencyStore = idempotencyStore;
    }

    public boolean handle(OrderProcessedEvent event) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable(
                CorrelationIdFilter.MDC_KEY, event.correlationId())) {
            if (!idempotencyStore.claim(event.eventId())) {
                logger.info("Duplicate event ignored: eventId={}, orderId={}", event.eventId(), event.orderId());
                return false;
            }
            logger.info("Order processed: orderId={}, eventId={}, status={}, processedAt={}, eventVersion={}",
                    event.orderId(), event.eventId(), event.status(), event.processedAt(), event.eventVersion());
            return true;
        }
    }
}
