package rw.ac.auca.scoutpro_27202.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.scoutpro_27202.domain.Athlete;

import java.util.Optional;
import java.util.UUID;

public interface AthleteRepository extends JpaRepository<Athlete, UUID> {

    Optional<Athlete> findByAthleteCode(String athleteCode);      // duplicate code check on update

    boolean existsByAthleteCode(String athleteCode);              // duplicate code check on create

    Optional<Athlete> findByUserId(UUID userId);                  // athlete views own profile (security)

    Page<Athlete> findBySportId(UUID sportId, Pageable pageable); // filter athletes by sport (later)

    boolean existsBySportId(UUID sportId);                        // can a sport be deleted?

    boolean existsByTeamId(UUID teamId);                          // can a team be deleted?
}