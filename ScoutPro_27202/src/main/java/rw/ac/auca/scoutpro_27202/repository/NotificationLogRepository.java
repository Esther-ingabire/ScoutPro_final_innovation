package rw.ac.auca.scoutpro_27202.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import rw.ac.auca.scoutpro_27202.document.NotificationChannel;
import rw.ac.auca.scoutpro_27202.document.NotificationLog;

public interface NotificationLogRepository extends MongoRepository<NotificationLog, String> {

    // idempotency: was this event already handled on this channel?
    boolean existsByEventIdAndChannel(String eventId, NotificationChannel channel);
}