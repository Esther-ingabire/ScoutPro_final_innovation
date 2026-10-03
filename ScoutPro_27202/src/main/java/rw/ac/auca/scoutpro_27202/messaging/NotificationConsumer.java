package rw.ac.auca.scoutpro_27202.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import rw.ac.auca.scoutpro_27202.document.AuditLog;
import rw.ac.auca.scoutpro_27202.document.NotificationChannel;
import rw.ac.auca.scoutpro_27202.document.NotificationLog;
import rw.ac.auca.scoutpro_27202.document.NotificationStatus;
import rw.ac.auca.scoutpro_27202.repository.AuditLogRepository;
import rw.ac.auca.scoutpro_27202.repository.NotificationLogRepository;

import java.util.Map;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    @Autowired
    private EmailSender emailSender;

    @Autowired
    private SmsSender smsSender;

    @Autowired
    private NotificationLogRepository notificationLogRepo;

    @Autowired
    private AuditLogRepository auditLogRepo;

    // the content of one email
    private record EmailMessage(String to, String subject, String body) {}

    // ---------------- EMAIL ----------------

    @RabbitListener(queues = RabbitConfig.EMAIL_QUEUE)
    public void handleEmail(DomainEvent event) {
        if (alreadyHandled(event, NotificationChannel.EMAIL)) {
            return;
        }

        EmailMessage email = buildEmail(event);
        if (email == null || email.to() == null || email.to().isBlank()) {
            saveLog(event, NotificationChannel.EMAIL, null, NotificationStatus.SKIPPED,
                    "No email address for this event");
            return;
        }

        // if this throws, RabbitMQ retries (2s, 4s, 8s), then moves the message to scoutpro.email.dlq
        emailSender.send(email.to(), email.subject(), email.body());
        saveLog(event, NotificationChannel.EMAIL, email.to(), NotificationStatus.SENT, null);
        log.info("[EMAIL SENT] {} to {}", event.type(), email.to());
    }

    private EmailMessage buildEmail(DomainEvent event) {
        Map<String, String> d = event.data();
        return switch (event.type()) {
            case "user.registered" -> new EmailMessage(
                    d.get("email"),
                    "Welcome to ScoutPro",
                    "Your ScoutPro account has been created.\n"
                            + "An administrator will assign your role shortly.");
            case "assessment.created" -> new EmailMessage(
                    d.get("athleteEmail"),
                    "New assessment recorded",
                    "Hello " + d.get("athleteName") + ",\n\n"
                            + "Scout " + d.get("scoutName") + " assessed you on " + d.get("assessmentDate")
                            + ".\nOverall score: " + d.get("overallScore") + " / 100.");
            case "shortlist.athleteadded" -> new EmailMessage(
                    d.get("athleteEmail"),
                    "You have been shortlisted!",
                    "Hello " + d.get("athleteName") + ",\n\n"
                            + "A club has added you to the shortlist \"" + d.get("shortlistName") + "\".");
            default -> null;   // e.g. athlete.deactivated: no recipient yet
        };
    }

    // ---------------- SMS ----------------

    @RabbitListener(queues = RabbitConfig.SMS_QUEUE)
    public void handleSms(DomainEvent event) {
        if (alreadyHandled(event, NotificationChannel.SMS)) {
            return;
        }

        String phone = event.data().get("athletePhone");
        if (phone == null || phone.isBlank()) {
            saveLog(event, NotificationChannel.SMS, null, NotificationStatus.SKIPPED,
                    "No phone number for this event");
            return;
        }

        String text = "ScoutPro: " + event.data().get("athleteName")
                + ", you were added to the shortlist \"" + event.data().get("shortlistName") + "\".";
        smsSender.send(phone, text);
        saveLog(event, NotificationChannel.SMS, phone, NotificationStatus.SENT, null);
    }

    // ---------------- AUDIT ----------------

    @RabbitListener(queues = RabbitConfig.AUDIT_QUEUE)
    public void handleAudit(DomainEvent event) {
        if (auditLogRepo.existsByEventId(event.eventId())) {
            log.info("Duplicate audit event {} ignored", event.eventId());
            return;
        }
        auditLogRepo.save(new AuditLog(event.eventId(), event.type(), event.data(), event.occurredAt()));
        log.info("[AUDIT] {} recorded", event.type());
    }

    // ---------------- helpers ----------------

    // idempotency: RabbitMQ may deliver the same message twice; never notify twice
    private boolean alreadyHandled(DomainEvent event, NotificationChannel channel) {
        if (notificationLogRepo.existsByEventIdAndChannel(event.eventId(), channel)) {
            log.info("Duplicate {} for event {} ignored", channel, event.eventId());
            return true;
        }
        return false;
    }

    private void saveLog(DomainEvent event, NotificationChannel channel, String recipient,
                         NotificationStatus status, String note) {
        try {
            notificationLogRepo.save(new NotificationLog(
                    event.eventId(), event.type(), channel, recipient, status, note));
        } catch (DuplicateKeyException e) {
            // another delivery of the same event logged it first: nothing to do
        }
    }
}