package rw.ac.auca.scoutpro_27202.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RabbitEventRelay {

    private static final Logger log = LoggerFactory.getLogger(RabbitEventRelay.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // runs only AFTER the database transaction commits (or immediately if there is none)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void relay(DomainEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, event.type(), event);
            log.info("Published {} ({})", event.type(), event.eventId());
        } catch (AmqpException e) {
            // RabbitMQ is down: the user's action already succeeded, so don't fail it
            log.error("Could not publish {} ({}): {}", event.type(), event.eventId(), e.getMessage());
        }
    }
}