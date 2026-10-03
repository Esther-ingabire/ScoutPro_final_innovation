package rw.ac.auca.scoutpro_27202.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.scoutpro_27202.domain.Criterion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CriterionRepository extends JpaRepository<Criterion, UUID> {

    List<Criterion> findBySportId(UUID sportId);                                // list a sport's criteria

    Optional<Criterion> findBySportIdAndNameIgnoreCase(UUID sportId, String name); // duplicate check

    boolean existsBySportId(UUID sportId);                                      // can a sport be deleted?
}