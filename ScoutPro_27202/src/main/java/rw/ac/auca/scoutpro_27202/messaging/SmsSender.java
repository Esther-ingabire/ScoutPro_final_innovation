package rw.ac.auca.scoutpro_27202.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmsSender {

    private static final Logger log = LoggerFactory.getLogger(SmsSender.class);

    public void send(String phoneNumber, String message) {
        // Development: log the SMS instead of sending it.
        // Production: call the Africa's Talking SMS API here (needs an account and API key).
        log.info("[SMS SENT] to {}: {}", phoneNumber, message);
    }
}