package rw.ac.auca.scoutpro_27202.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.scoutpro_27202.domain.AssessmentScore;

import java.util.List;
import java.util.UUID;

public interface AssessmentScoreRepository extends JpaRepository<AssessmentScore, UUID> {
    List<AssessmentScore> findByAssessmentId(UUID assessmentId);
    boolean existsByCriterionId(UUID criterionId);
}