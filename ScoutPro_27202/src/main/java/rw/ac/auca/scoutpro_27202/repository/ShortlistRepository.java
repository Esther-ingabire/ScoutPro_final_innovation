package rw.ac.auca.scoutpro_27202.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.scoutpro_27202.domain.Shortlist;

import java.util.List;
import java.util.UUID;

public interface ShortlistRepository extends JpaRepository<Shortlist, UUID> {

    List<Shortlist> findByOwnerId(UUID ownerId);               // manager sees own lists

    boolean existsByOwnerIdAndName(UUID ownerId, String name); // duplicate list name check

    boolean existsByAthletes_Id(UUID athleteId);               // can an athlete be deleted?
}