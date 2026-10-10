package rw.ac.auca.scoutpro_27202.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.ac.auca.scoutpro_27202.domain.Assessment;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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

    // every assessment of one athlete, with the scout loaded, so deactivation can email them
    @Query("select distinct a from Assessment a join fetch a.scout where a.athlete.id = :athleteId")
    List<Assessment> findWithScoutByAthleteId(@Param("athleteId") UUID athleteId);

    // One assessment with the names and scores needed for the PDF.
    @Query("""
            select distinct a from Assessment a
            join fetch a.athlete athlete
            join fetch athlete.sport
            join fetch a.scout
            left join fetch a.scores score
            left join fetch score.criterion
            where a.id = :id
            """)
    Optional<Assessment> findDetailedById(@Param("id") UUID id);
}