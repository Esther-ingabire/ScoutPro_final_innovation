package rw.ac.auca.scoutpro_27202.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import rw.ac.auca.scoutpro_27202.document.AuditLog;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    boolean existsByEventId(String eventId);
}