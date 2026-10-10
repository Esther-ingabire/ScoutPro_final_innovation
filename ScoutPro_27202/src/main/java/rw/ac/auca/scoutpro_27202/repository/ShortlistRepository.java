package rw.ac.auca.scoutpro_27202.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.scoutpro_27202.domain.Shortlist;

import java.util.UUID;

public interface ShortlistRepository extends JpaRepository<Shortlist, UUID> {

    Page<Shortlist> findByOwnerId(UUID ownerId, Pageable pageable);   // manager sees own lists

    boolean existsByOwnerIdAndName(UUID ownerId, String name); // duplicate list name check

    boolean existsByAthletes_Id(UUID athleteId);               // can an athlete be deleted?
}