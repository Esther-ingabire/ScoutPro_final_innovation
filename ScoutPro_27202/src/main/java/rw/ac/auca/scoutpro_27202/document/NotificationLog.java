package rw.ac.auca.scoutpro_27202.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "notification_logs")
@CompoundIndex(name = "event_channel_unique", def = "{'eventId': 1, 'channel': 1}", unique = true)
public class NotificationLog {

    @Id
    private String id;

    private String eventId;               // which event caused this notification

    private String eventType;             // e.g. shortlist.athleteadded

    private NotificationChannel channel;  // EMAIL or SMS

    private String recipient;             // email address or phone number

    private NotificationStatus status;

    private String note;                  // reason when SKIPPED

    private Instant sentAt;

    public NotificationLog() {}

    public NotificationLog(String eventId, String eventType, NotificationChannel channel,
                           String recipient, NotificationStatus status, String note) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.channel = channel;
        this.recipient = recipient;
        this.status = status;
        this.note = note;
        this.sentAt = Instant.now();
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }
}