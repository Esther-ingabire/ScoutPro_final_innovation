package rw.ac.auca.scoutpro_27202.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "scoutpro.events";   // where every event is published
    public static final String DLX = "scoutpro.dlx";           // where failed messages go

    public static final String EMAIL_QUEUE = "scoutpro.email";
    public static final String SMS_QUEUE = "scoutpro.sms";
    public static final String AUDIT_QUEUE = "scoutpro.audit";

    // all exchanges, queues and bindings, declared in RabbitMQ at startup
    @Bean
    public Declarables topology() {
        TopicExchange events = new TopicExchange(EXCHANGE, true, false);
        DirectExchange dlx = new DirectExchange(DLX, true, false);

        Queue email = queueWithDeadLetter(EMAIL_QUEUE);
        Queue sms = queueWithDeadLetter(SMS_QUEUE);
        Queue audit = queueWithDeadLetter(AUDIT_QUEUE);

        Queue emailDlq = QueueBuilder.durable(EMAIL_QUEUE + ".dlq").build();
        Queue smsDlq = QueueBuilder.durable(SMS_QUEUE + ".dlq").build();
        Queue auditDlq = QueueBuilder.durable(AUDIT_QUEUE + ".dlq").build();

        return new Declarables(
                events, dlx,
                email, sms, audit,
                emailDlq, smsDlq, auditDlq,

                // which events reach which queue (routing table from the design doc)
                BindingBuilder.bind(email).to(events).with("user.registered"),
                BindingBuilder.bind(email).to(events).with("user.otp.sent"),
                BindingBuilder.bind(email).to(events).with("assessment.created"),
                BindingBuilder.bind(email).to(events).with("shortlist.athleteadded"),
                BindingBuilder.bind(email).to(events).with("athlete.deactivated"),

                BindingBuilder.bind(sms).to(events).with("shortlist.athleteadded"),

                BindingBuilder.bind(audit).to(events).with("assessment.created"),
                BindingBuilder.bind(audit).to(events).with("athlete.deactivated"),
                BindingBuilder.bind(audit).to(events).with("entity.changed"),

                // failed messages: dlx → matching .dlq queue
                BindingBuilder.bind(emailDlq).to(dlx).with(EMAIL_QUEUE + ".dlq"),
                BindingBuilder.bind(smsDlq).to(dlx).with(SMS_QUEUE + ".dlq"),
                BindingBuilder.bind(auditDlq).to(dlx).with(AUDIT_QUEUE + ".dlq")
        );
    }

    // a durable queue that sends rejected messages to the DLX with key "<queue>.dlq"
    private Queue queueWithDeadLetter(String name) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(name + ".dlq")
                .build();
    }

    // send and receive messages as JSON instead of Java serialization
    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}