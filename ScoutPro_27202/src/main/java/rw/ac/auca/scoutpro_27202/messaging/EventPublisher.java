package rw.ac.auca.scoutpro_27202.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EventPublisher {

    @Autowired
    private ApplicationEventPublisher springEvents;

    // announce an event inside the app; RabbitEventRelay forwards it after commit
    public void publish(String type, Map<String, String> data) {
        springEvents.publishEvent(DomainEvent.of(type, data));
    }
}