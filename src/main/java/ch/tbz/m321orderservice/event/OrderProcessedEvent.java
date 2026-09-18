package ch.tbz.m321orderservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderProcessedEvent(
        UUID eventId,
        String correlationId,
        UUID orderId,
        Instant processedAt,
        String status,
        int eventVersion) {
}
