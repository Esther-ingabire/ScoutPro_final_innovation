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

import java.util.ArrayList;
import java.util.List;
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

        List<EmailMessage> emails = buildEmails(event);
        if (emails.isEmpty()) {
            saveLog(event, NotificationChannel.EMAIL, null, NotificationStatus.SKIPPED,
                    "No email address for this event");
            return;
        }

        // if this throws, RabbitMQ retries (2s, 4s, 8s), then moves the message to scoutpro.email.dlq.
        // A retry can send an earlier address in this list a second time. The log below is written
        // only after every send succeeds, which is what alreadyHandled checks.
        StringBuilder sentTo = new StringBuilder();
        for (EmailMessage email : emails) {
            emailSender.send(email.to(), email.subject(), email.body());
            if (!sentTo.isEmpty()) {
                sentTo.append(',');
            }
            sentTo.append(email.to());
            log.info("[EMAIL SENT] {} to {}", event.type(), email.to());
        }
        saveLog(event, NotificationChannel.EMAIL, sentTo.toString(), NotificationStatus.SENT, null);
    }

    private List<EmailMessage> buildEmails(DomainEvent event) {
        Map<String, String> d = event.data();
        return switch (event.type()) {
            case "user.registered" -> withAddress(List.of(
                    new EmailMessage(
                            d.get("email"),
                            "Welcome to ScoutPro",
                            welcomeBody(d)),
                    new EmailMessage(
                            d.get("adminEmail"),
                            "New ScoutPro account to approve",
                            "A new account was created for " + d.get("email") + ".\n"
                                    + "Sign in as an administrator and assign a role. "
                                    + "New accounts start as ATHLETE.")));
            case "user.otp.sent" -> withAddress(List.of(new EmailMessage(
                    d.get("email"),
                    "Your ScoutPro confirmation code",
                    codeBody(d.get("otp")))));
            case "assessment.created" -> withAddress(List.of(new EmailMessage(
                    d.get("athleteEmail"),
                    "New assessment recorded",
                    "Hello " + d.get("athleteName") + ",\n\n"
                            + "Scout " + d.get("scoutName") + " assessed you on " + d.get("assessmentDate")
                            + ".\nOverall score: " + d.get("overallScore") + " / 100.")));
            case "shortlist.athleteadded" -> withAddress(List.of(new EmailMessage(
                    d.get("athleteEmail"),
                    "You have been shortlisted!",
                    "Hello " + d.get("athleteName") + ",\n\n"
                            + "A club has added you to the shortlist \"" + d.get("shortlistName") + "\".")));
            case "athlete.deactivated" -> split(d.get("scoutEmails")).stream()
                    .map(to -> new EmailMessage(
                            to,
                            "Athlete deactivated",
                            d.get("athleteName") + " was deactivated. "
                                    + "Assessments you recorded for them stay in ScoutPro."))
                    .toList();
            default -> List.of();
        };
    }

    // The code stays in the user's email. The admin alert never receives it.
    private String welcomeBody(Map<String, String> data) {
        String code = data.get("otp");
        if (code == null || code.isBlank()) {
            return "Your ScoutPro account has been created.\n"
                    + "An administrator will assign your role shortly.";
        }
        return codeBody(code)
                + "\n\nAn administrator will assign your role shortly.";
    }

    private String codeBody(String code) {
        return "Your ScoutPro confirmation code is " + code + ".\n"
                + "It expires in 10 minutes.\n"
                + "Enter it on the confirmation page before you sign in.";
    }

    private List<EmailMessage> withAddress(List<EmailMessage> emails) {
        List<EmailMessage> kept = new ArrayList<>();
        for (EmailMessage email : emails) {
            if (email.to() != null && !email.to().isBlank()) {
                kept.add(email);
            }
        }
        return kept;
    }

    private List<String> split(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        List<String> addresses = new ArrayList<>();
        for (String part : csv.split(",")) {
            if (!part.isBlank()) {
                addresses.add(part.trim());
            }
        }
        return addresses;
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