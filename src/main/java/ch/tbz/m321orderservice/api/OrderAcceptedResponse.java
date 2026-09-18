package ch.tbz.m321orderservice.api;

import java.time.Instant;
import java.util.UUID;

public record OrderAcceptedResponse(UUID orderId, String correlationId, String status, Instant acceptedAt) {
}
