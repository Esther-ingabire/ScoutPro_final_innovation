package rw.ac.auca.scoutpro_27202.messaging;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

// one class for all events; "type" is also the routing key
public record DomainEvent(String eventId, String type, Instant occurredAt, Map<String, String> data) {

    public static DomainEvent of(String type, Map<String, String> data) {
        return new DomainEvent(UUID.randomUUID().toString(), type, Instant.now(), data);
    }
}