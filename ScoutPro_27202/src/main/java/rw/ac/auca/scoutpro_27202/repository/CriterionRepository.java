package rw.ac.auca.scoutpro_27202.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.scoutpro_27202.domain.Criterion;

import java.util.List;
import java.util.UUID;

public interface CriterionRepository extends JpaRepository<Criterion, UUID> {
    List<Criterion> findBySportId(UUID sportId);              // FR4: criteria to score
    boolean existsBySportIdAndName(UUID sportId, String name);
}