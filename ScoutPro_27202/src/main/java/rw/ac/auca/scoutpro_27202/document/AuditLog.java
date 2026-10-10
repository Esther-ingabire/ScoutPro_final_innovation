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

    private String actorId;            // who did it (user id, or "system")

    @Indexed
    private String entityId;

    private String before;             // JSON snapshot before the change

    private String after;              // JSON snapshot after the change

    private Map<String, String> data;  // the event's details

    @Indexed(expireAfter = "365d")
    private Instant at;                // MongoDB deletes the entry automatically after 365 days

    public AuditLog() {}

    public AuditLog(String eventId, String action, Map<String, String> data, Instant at) {
        this.eventId = eventId;
        this.action = action;
        this.entity = data != null && data.get("entity") != null
                ? data.get("entity")
                : (action.contains(".") ? action.substring(0, action.indexOf('.')) : action);
        this.actorId = data == null ? null : data.get("actorId");
        this.entityId = data == null ? null : data.get("entityId");
        this.before = data == null ? null : data.get("before");
        this.after = data == null ? null : data.get("after");
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

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getBefore() {
        return before;
    }

    public void setBefore(String before) {
        this.before = before;
    }

    public String getAfter() {
        return after;
    }

    public void setAfter(String after) {
        this.after = after;
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