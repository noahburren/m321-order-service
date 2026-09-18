package ch.tbz.m321orderservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCreatedEvent(
        UUID eventId,
        String correlationId,
        UUID orderId,
        String product,
        int quantity,
        Instant createdAt,
        int eventVersion) {
}
