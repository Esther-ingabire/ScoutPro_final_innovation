package rw.ac.auca.scoutpro_27202.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.scoutpro_27202.domain.Athlete;

import java.util.Optional;
import java.util.UUID;

public interface AthleteRepository extends JpaRepository<Athlete, UUID> {
    Optional<Athlete> findByAthleteCode(String athleteCode);
    boolean existsByAthleteCode(String athleteCode);
    Optional<Athlete> findByUserId(UUID userId);              // athlete sees own profile
    Page<Athlete> findBySportId(UUID sportId, Pageable pageable);
    boolean existsBySportId(UUID sportId);
}
