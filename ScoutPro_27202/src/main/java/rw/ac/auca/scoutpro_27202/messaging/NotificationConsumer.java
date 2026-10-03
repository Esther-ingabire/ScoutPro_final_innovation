package rw.ac.auca.scoutpro_27202.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// step 1: only log what arrives; next step: send real email/SMS and write MongoDB logs
@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    @RabbitListener(queues = RabbitConfig.EMAIL_QUEUE)
    public void handleEmail(DomainEvent event) {
        log.info("[EMAIL] {} -> {}", event.type(), event.data());
    }

    @RabbitListener(queues = RabbitConfig.SMS_QUEUE)
    public void handleSms(DomainEvent event) {
        log.info("[SMS] {} -> {}", event.type(), event.data());
    }

    @RabbitListener(queues = RabbitConfig.AUDIT_QUEUE)
    public void handleAudit(DomainEvent event) {
        log.info("[AUDIT] {} -> {}", event.type(), event.data());
    }
}