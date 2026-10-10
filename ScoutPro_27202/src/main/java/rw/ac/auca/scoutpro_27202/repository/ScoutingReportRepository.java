package rw.ac.auca.scoutpro_27202.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.repository.MongoRepository;
import rw.ac.auca.scoutpro_27202.document.ScoutingReport;

import java.util.Optional;

public interface ScoutingReportRepository extends MongoRepository<ScoutingReport, String> {

    Optional<ScoutingReport> findByAssessmentId(String assessmentId);

    boolean existsByAssessmentId(String assessmentId);

    Page<ScoutingReport> findByAthleteIdOrderByCreatedAtDesc(String athleteId, Pageable pageable);

    // full-text search over summary + tags
    Page<ScoutingReport> findAllBy(TextCriteria criteria, Pageable pageable);

    void deleteByAssessmentId(String assessmentId);
}