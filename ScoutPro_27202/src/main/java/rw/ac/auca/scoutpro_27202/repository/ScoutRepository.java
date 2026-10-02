package rw.ac.auca.scoutpro_27202.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.scoutpro_27202.domain.Scout;

import java.util.Optional;
import java.util.UUID;

public interface ScoutRepository extends JpaRepository<Scout, UUID> {
    Optional<Scout> findByUserId(UUID userId);                // "which scout is logged in?"
    Optional<Scout> findByScoutCode(String scoutCode);
    boolean existsByEmail(String email);
}