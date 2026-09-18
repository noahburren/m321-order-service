package ch.tbz.m321orderservice.idempotency;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryEventIdempotencyStore implements EventIdempotencyStore {

    private final Set<UUID> processedEventIds = ConcurrentHashMap.newKeySet();

    @Override
    public boolean claim(UUID eventId) {
        return processedEventIds.add(eventId);
    }
}
