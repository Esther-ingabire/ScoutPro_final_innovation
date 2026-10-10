package rw.ac.auca.scoutpro_27202.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.scoutpro_27202.domain.Team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {

    List<Team> findBySportId(UUID sportId);                                   // list a sport's teams

    Page<Team> findBySportId(UUID sportId, Pageable pageable);

    boolean existsBySportId(UUID sportId);                                    // can a sport be deleted?

    Optional<Team> findBySportIdAndNameIgnoreCase(UUID sportId, String name); // duplicate team name check
}