package rw.ac.auca.scoutpro_27202.repository;

import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.repository.MongoRepository;
import rw.ac.auca.scoutpro_27202.document.ScoutingReport;

import java.util.List;
import java.util.Optional;

public interface ScoutingReportRepository extends MongoRepository<ScoutingReport, String> {

    Optional<ScoutingReport> findByAssessmentId(String assessmentId);

    boolean existsByAssessmentId(String assessmentId);

    List<ScoutingReport> findByAthleteIdOrderByCreatedAtDesc(String athleteId);

    // full-text search over summary + tags
    List<ScoutingReport> findAllBy(TextCriteria criteria);

    void deleteByAssessmentId(String assessmentId);
}