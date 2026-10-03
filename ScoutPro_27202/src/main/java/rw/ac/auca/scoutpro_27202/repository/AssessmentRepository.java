package rw.ac.auca.scoutpro_27202.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.scoutpro_27202.domain.Assessment;

import java.time.LocalDate;
import java.util.UUID;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {

    // US1: same-day rule
    boolean existsByScoutIdAndAthleteIdAndAssessmentDate(UUID scoutId, UUID athleteId, LocalDate date);

    // athlete profile: history, newest first
    Page<Assessment> findByAthleteIdOrderByAssessmentDateDesc(UUID athleteId, Pageable pageable);

    // simple ranking, highest score first
    Page<Assessment> findAllByOrderByOverallScoreDesc(Pageable pageable);

    // can an athlete be deleted?
    boolean existsByAthleteId(UUID athleteId);

    boolean existsByScoutId(UUID scoutId);   // can a scout be deleted?
}