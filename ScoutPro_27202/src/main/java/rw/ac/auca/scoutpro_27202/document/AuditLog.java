package rw.ac.auca.scoutpro_27202.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    @Indexed(unique = true)
    private String eventId;            // each event is audited once

    private String action;             // e.g. assessment.created

    @Indexed
    private String entity;             // e.g. assessment, athlete

    private Map<String, String> data;  // the event's details

    @Indexed(expireAfter = "365d")
    private Instant at;                // MongoDB deletes the entry automatically after 365 days

    public AuditLog() {}

    public AuditLog(String eventId, String action, Map<String, String> data, Instant at) {
        this.eventId = eventId;
        this.action = action;
        this.entity = action.contains(".") ? action.substring(0, action.indexOf('.')) : action;
        this.data = data;
        this.at = at;
    }



    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    public Instant getAt() {
        return at;
    }

    public void setAt(Instant at) {
        this.at = at;
    }
}