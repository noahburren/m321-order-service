package ch.tbz.m321orderservice.idempotency;

import java.util.UUID;

public interface EventIdempotencyStore {
    boolean claim(UUID eventId);
}
