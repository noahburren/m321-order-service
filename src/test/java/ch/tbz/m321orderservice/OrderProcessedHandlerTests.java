package ch.tbz.m321orderservice;

import static org.assertj.core.api.Assertions.assertThat;

import ch.tbz.m321orderservice.event.OrderProcessedEvent;
import ch.tbz.m321orderservice.idempotency.InMemoryEventIdempotencyStore;
import ch.tbz.m321orderservice.service.OrderProcessedHandler;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderProcessedHandlerTests {

    @Test
    void duplicateIncomingEventIsProcessedOnlyOnce() {
        OrderProcessedHandler handler = new OrderProcessedHandler(new InMemoryEventIdempotencyStore());
        OrderProcessedEvent event = new OrderProcessedEvent(
                UUID.randomUUID(), "correlation-1", UUID.randomUUID(),
                Instant.parse("2026-09-18T10:00:00Z"), "PROCESSED", 1);
        assertThat(handler.handle(event)).isTrue();
        assertThat(handler.handle(event)).isFalse();
    }
}
