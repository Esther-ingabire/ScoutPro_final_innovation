package rw.ac.auca.scoutpro_27202.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rw.ac.auca.scoutpro_27202.security.CurrentUser;

import java.util.HashMap;
import java.util.Map;

// Publishes entity.changed after a create, update or delete.
// RabbitEventRelay forwards it only after the database commit, and the audit consumer
// writes it to MongoDB. The HTTP request does not wait for MongoDB.
@Component
public class AuditRecorder {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private CurrentUser currentUser;

    public void changed(String entity, String entityId, String action, String before, String after) {
        publish(actor(), entity, entityId, action, before, after);
    }

    // Used at registration, where there is no JWT yet. The new user's id is the actor.
    public void changedAs(String actorId, String entity, String entityId, String action, String before, String after) {
        publish(actorId, entity, entityId, action, before, after);
    }

    private void publish(String actorId, String entity, String entityId, String action, String before, String after) {
        Map<String, String> data = new HashMap<>();
        data.put("actorId", actorId);
        data.put("entity", entity);
        data.put("entityId", entityId);
        data.put("action", action);
        if (before != null) {
            data.put("before", before);
        }
        if (after != null) {
            data.put("after", after);
        }
        eventPublisher.publish("entity.changed", data);
    }

    private String actor() {
        try {
            return currentUser.getId().toString();
        } catch (RuntimeException ex) {
            return "system";
        }
    }
}
