package rw.ac.auca.scoutpro_27202.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.scoutpro_27202.domain.Criterion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CriterionRepository extends JpaRepository<Criterion, UUID> {

    List<Criterion> findBySportId(UUID sportId);                                // all criteria, used when scoring

    Page<Criterion> findBySportId(UUID sportId, Pageable pageable);            // paged list for the UI

    Optional<Criterion> findBySportIdAndNameIgnoreCase(UUID sportId, String name); // duplicate check

    boolean existsBySportId(UUID sportId);                                      // can a sport be deleted?
}